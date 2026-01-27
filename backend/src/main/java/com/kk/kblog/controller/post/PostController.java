package com.kk.kblog.controller.post;

import com.kk.kblog.dto.post.HotPostDto;
import com.kk.kblog.dto.post.PostDto;
import com.kk.kblog.dto.post.PostRequests.CreatePostRequest;
import com.kk.kblog.dto.post.PostRequests.PatchPostRequest;
import com.kk.kblog.dto.post.PostRequests.UpdatePostRequest;
import com.kk.kblog.service.HotPostsService;
import com.kk.kblog.service.post.PostService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final HotPostsService hotPostsService;

    public PostController(PostService postService, HotPostsService hotPostsService) {
        this.postService = postService;
        this.hotPostsService = hotPostsService;
    }

    @GetMapping
    public List<PostDto> list(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (page == null && size == null) {
            return postService.listPosts();
        }
        int p = page == null ? 0 : page;
        int s = size == null ? 5 : size;
        return postService.listPosts(p, s);
    }

    @GetMapping("/{id}")
    public PostDto get(@PathVariable long id) {
        // 异步记录文章访问（不阻塞响应）
        CompletableFuture.runAsync(() -> hotPostsService.recordPostView(id));
        return postService.getPost(id);
    }

    @PostMapping
    public ResponseEntity<PostDto> create(@Valid @RequestBody CreatePostRequest request) {
        var created = postService.createPost(request);
        return ResponseEntity.created(URI.create("/api/posts/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PostDto update(@PathVariable long id, @Valid @RequestBody UpdatePostRequest request) {
        return postService.updatePost(id, request);
    }

    @PatchMapping("/{id}")
    public PostDto patch(@PathVariable long id, @Valid @RequestBody PatchPostRequest request) {
        return postService.patchPost(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/views")
    public PostDto incrementViews(@PathVariable long id) {
        return postService.incrementViews(id);
    }

    @PostMapping("/{id}/likes")
    public PostDto incrementLikes(@PathVariable long id) {
        return postService.incrementLikes(id);
    }

    @PostMapping("/{id}/comments/increment")
    public PostDto incrementCommentsLegacy(@PathVariable long id) {
        return postService.incrementComments(id);
    }

    /**
     * 获取热门文章榜单
     * 返回最近时间窗口内访问过的文章，按最近访问时间排序
     */
    @GetMapping("/hot")
    public List<HotPostDto> getHotPosts() {
        return hotPostsService.getHotPosts();
    }
}
