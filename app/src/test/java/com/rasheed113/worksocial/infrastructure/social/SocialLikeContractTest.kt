package com.rasheed113.worksocial.infrastructure.social

import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.LikeMutationResult
import com.rasheed113.worksocial.domain.social.SocialHomeState
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostAuthor
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import com.rasheed113.worksocial.presentation.social.SocialHomeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialLikeContractTest {
 @After fun tearDown(){ Dispatchers.resetMain() }
 private fun post(id:String,liked:Boolean=false,count:Int=0)=SocialPost(id,"author-$id","post $id","public",null,null,null,"2026-08-31T00:00:00Z","2026-08-31T00:00:00Z",SocialPostAuthor("user$id","User $id"),emptyList(),count,liked)
 @Test fun initialLikedStateAndCountAreMappedFromLikeRows(){ val result=applyLikeState(listOf(post("a")),listOf(PostReactionDto("a","current"),PostReactionDto("a","other")),"current"); assertEquals(2,result.single().likeCount); assertTrue(result.single().isLikedByCurrentUser) }
 @Test fun initialUnlikedStateAndZeroCountAreMappedWithoutFabrication(){ val result=applyLikeState(listOf(post("a")),listOf(PostReactionDto("a","other")),"current"); assertEquals(1,result.single().likeCount); assertFalse(result.single().isLikedByCurrentUser) }
 @Test fun likeAndUnlikeResultsRepresentBackendState()=runTest{ val fake=FakeRepository(likeResult=LikeMutationResult.Success(4,true),unlikeResult=LikeMutationResult.Success(3,false)); assertEquals(4,(fake.likePost("a") as LikeMutationResult.Success).likeCount); assertEquals(3,(fake.unlikePost("a") as LikeMutationResult.Success).likeCount) }
 @Test fun backendFailuresRemainFailures()=runTest{ val fake=FakeRepository(likeResult=LikeMutationResult.Failure("RLS rejected"),unlikeResult=LikeMutationResult.Failure("network")); assertTrue(fake.likePost("a") is LikeMutationResult.Failure); assertTrue(fake.unlikePost("a") is LikeMutationResult.Failure) }
 @Test fun viewModelPreventsDuplicateLikeSubmissionsPerPostAndAppliesResult()=runTest{ Dispatchers.setMain(Dispatchers.Unconfined); val release=CompletableDeferred<Unit>(); val fake=FakeRepository(posts=listOf(post("a")),likeResult=LikeMutationResult.Success(1,true),release=release); val vm=SocialHomeViewModel(fake); vm.load(); vm.toggleLike("a"); vm.toggleLike("a"); assertEquals(1,fake.likeCalls); assertTrue((vm.state.value as SocialHomeState.Success).likingPostIds.contains("a")); release.complete(Unit); val s=vm.state.value as SocialHomeState.Success; assertEquals(1,s.posts.single().likeCount); assertTrue(s.posts.single().isLikedByCurrentUser); assertTrue(s.likingPostIds.isEmpty()) }
 @Test fun viewModelKeepsOtherPostsInteractiveWhileOneMutationIsPending()=runTest{ Dispatchers.setMain(Dispatchers.Unconfined); val release=CompletableDeferred<Unit>(); val fake=FakeRepository(posts=listOf(post("a"),post("b")),release=release); val vm=SocialHomeViewModel(fake); vm.load(); vm.toggleLike("a"); vm.toggleLike("b"); val p=vm.state.value as SocialHomeState.Success; assertEquals(setOf("a","b"),p.likingPostIds); assertEquals(1,fake.likeCallsFor("a")); assertEquals(1,fake.likeCallsFor("b")); release.complete(Unit) }
 @Test fun viewModelDoesNotShowLikedAfterBackendFailure()=runTest{ Dispatchers.setMain(Dispatchers.Unconfined); val fake=FakeRepository(posts=listOf(post("a")),likeResult=LikeMutationResult.Failure("network")); val vm=SocialHomeViewModel(fake); vm.load(); vm.toggleLike("a"); val s=vm.state.value as SocialHomeState.Success; assertFalse(s.posts.single().isLikedByCurrentUser); assertEquals(0,s.posts.single().likeCount); assertEquals("network",s.actionError) }
}
private class FakeRepository(private var posts:List<SocialPost> = emptyList(),var likeResult:LikeMutationResult=LikeMutationResult.Success(0,true),private val unlikeResult:LikeMutationResult=LikeMutationResult.Success(0,false),private val release:CompletableDeferred<Unit>?=null):SocialPostRepository{ var likeCalls=0; private val calls=mutableMapOf<String,Int>(); override suspend fun getHomePosts()=posts; override suspend fun createPost(content:String):CreatePostResult=CreatePostResult.Failure("unused"); override suspend fun likePost(postId:String):LikeMutationResult{likeCalls++;calls[postId]=(calls[postId]?:0)+1;release?.await();return likeResult};override suspend fun unlikePost(postId:String)=unlikeResult;fun likeCallsFor(postId:String)=calls[postId]?:0 }
