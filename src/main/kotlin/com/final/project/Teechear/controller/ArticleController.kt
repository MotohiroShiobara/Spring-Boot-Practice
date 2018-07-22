package com.final.project.Teechear.controller

import com.final.project.Teechear.domain.Comment
import com.final.project.Teechear.entity.ArticleEntity
import com.final.project.Teechear.entity.UserLikeArticleEntity
import com.final.project.Teechear.mapper.ArticleMapper
import com.final.project.Teechear.mapper.UserLikeArticleMapper
import com.final.project.Teechear.mapper.UserMapper
import com.final.project.Teechear.service.ArticleService
import com.final.project.Teechear.service.CommentService
import com.final.project.Teechear.service.LikeService
import com.final.project.Teechear.form.ArticleForm
import com.final.project.Teechear.form.CommentForm
import com.final.project.Teechear.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.*

@Controller
@RequestMapping("/article")
class ArticleController(
        private val userMapper: UserMapper,
        private val userService: UserService,
        private val articleMapper: ArticleMapper,
        private val userLikeArticleMapper: UserLikeArticleMapper,
        private val likeService: LikeService,
        private val articleService: ArticleService,
        private val commentService: CommentService) {

    @GetMapping("/new")
    fun new(model: Model, principal: Principal): String {
        val currentUserId = userMapper.findByEmailOrName(principal.name)
        model.addAttribute("currentUserId", currentUserId)
        model.addAttribute("articleForm", ArticleForm())
        return "article/new"
    }

    @PostMapping("")
    fun create(
            principal: Principal,
            @Validated articleForm: ArticleForm,
            result: BindingResult
    ): String {
        val currentUser = userMapper.findByEmailOrName(principal.name)
        if (result.hasErrors()) {
            return "article/new"
        }

        val article = ArticleEntity(articleForm.title, currentUser?.id, Date(), "\n" + articleForm.markdownText)
        articleMapper.insert(article)
        return "redirect:/article/${article.id}"
    }

    @GetMapping("/{articleId}")
    fun show(@PathVariable("articleId") articleId: Int, model: Model, principal: Principal, commentForm: CommentForm): String {
        val article = articleService.find(articleId)
        val currentUser = userMapper.findByEmailOrName(principal.name)
        val currentUserId = currentUser?.id

        if (currentUserId is Int) {
            model.addAttribute("likeCount", userLikeArticleMapper.articleLikeCount(articleId))
            model.addAttribute("commentForm", commentForm)
            model.addAttribute("currentUserId", currentUserId)

            val commentList = commentService.commentListByArticle(articleId)
            model.addAttribute("commentList", commentList)
            model.addAttribute("userLiked", userLikeArticleMapper.findByUserIdAndArticleId(articleId, currentUserId) is UserLikeArticleEntity)
        }

        model.addAttribute("article", article)
        model.addAttribute("isMyArticle", article.userId == currentUserId)
        return "article/show"
    }

    @GetMapping("/{articleId}/comments.json")
    fun commentJson(@PathVariable("articleId") articleId: Int): ResponseEntity<List<Comment>> {
        val commentList = commentService.commentListByArticle(articleId)
        return ResponseEntity.ok(commentList)
    }

    @PostMapping("/{articleId}/like")
    fun like(@PathVariable("articleId") articleId: Int, principal: Principal): String {
        likeService.create(articleId, principal)

        return "redirect:/article/${articleId}"
    }

    @DeleteMapping("/{articleId}/unlike")
    fun unlike(@PathVariable("articleId") articleId: Int, principal: Principal): String {
        val user = userService.currentUser(principal)
        likeService.delete(articleId, user)
        return "redirect:/article/${articleId}"
    }
}