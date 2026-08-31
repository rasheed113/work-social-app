package com.rasheed113.worksocial.presentation.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.rasheed113.worksocial.domain.account.AccountProfile
import com.rasheed113.worksocial.domain.friends.RelationshipState
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import java.io.ByteArrayOutputStream

@Composable
fun ProfileScreen(accountRepository: com.rasheed113.worksocial.domain.account.AccountRepository, friendsRepository: com.rasheed113.worksocial.domain.friends.FriendsRepository, socialPostRepository: SocialPostRepository, currentUserId: String, targetProfileId: String?) {
    val vm: ProfileViewModel = viewModel(key="profile-${targetProfileId ?: "me"}", factory=ProfileViewModelFactory(accountRepository,friendsRepository,socialPostRepository,currentUserId,targetProfileId))
    val profileState by vm.profile.collectAsStateWithLifecycle(); val edit by vm.edit.collectAsStateWithLifecycle(); val relationship by vm.relationship.collectAsStateWithLifecycle(); val social by vm.social.collectAsStateWithLifecycle(); val posts by vm.posts.collectAsStateWithLifecycle()
    val context=LocalContext.current; var editing by remember { mutableStateOf(false) }; var confirmBlock by remember { mutableStateOf(false) }
    LaunchedEffect(edit.saved) { if(edit.saved) editing=false }
    if(confirmBlock) AlertDialog(onDismissRequest={confirmBlock=false}, title={Text("Block user?")}, text={Text("They will no longer be able to view or interact with you." )}, confirmButton={Button(onClick={confirmBlock=false;vm.blockProfile()},enabled=!social.busy){Text("Block")}}, dismissButton={TextButton(onClick={confirmBlock=false}){Text("Cancel")}})
    when(val state=profileState){
        ProfileLoadState.Loading -> ProfileStateCard("Loading profile…")
        ProfileLoadState.NotFound -> ProfileStateCard("Profile not found. No placeholder profile is shown.")
        ProfileLoadState.BlockedByMe -> ProfileBlockedCard(social.busy,social.error,vm::unblockProfile)
        is ProfileLoadState.Error -> ProfileErrorCard(state.message,vm::load)
        is ProfileLoadState.Success -> if(editing && vm.isOwnProfile) ProfileEditor(state.profile,edit,vm::updateEdit,vm::saveProfile,{editing=false},vm::uploadAvatar) else ProfileContent(state.profile,vm.isOwnProfile,relationship,social,posts,{editing=true},vm::toggleFollow,vm::sendFriendRequest,vm::acceptFriendRequest,vm::cancelFriendRequest,{confirmBlock=true},vm::load){url->context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}
    }
}

@Composable private fun ProfileContent(profile: AccountProfile, own: Boolean, relationship: ProfileRelationshipState, social: ProfileSocialState, posts: ProfilePostsState, onEdit:()->Unit, onFollow:()->Unit, onSend:()->Unit, onAccept:()->Unit, onCancel:()->Unit, onBlock:()->Unit, onRetry:()->Unit, onWebsite:(String)->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=androidx.compose.foundation.layout.PaddingValues(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Card(shape=RoundedCornerShape(22.dp),elevation=CardDefaults.cardElevation(3.dp)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){ProfileAvatar(profile.avatar_url,profile.display_name,112);Spacer(Modifier.height(12.dp));Text(profile.display_name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("@${profile.username}",color=MaterialTheme.colorScheme.onSurfaceVariant);profile.bio?.takeIf(String::isNotBlank)?.let{Text(it,Modifier.padding(top=8.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)};profile.location?.takeIf(String::isNotBlank)?.let{Text("📍 $it",Modifier.padding(top=7.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)};profile.website?.takeIf(String::isNotBlank)?.let{url->Text(url,Modifier.padding(top=6.dp).clickable{onWebsite(url)},color=MaterialTheme.colorScheme.primary,maxLines=1,overflow=TextOverflow.Ellipsis)};Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterHorizontally)){if(own)Button(onClick=onEdit){Text("Edit Profile")}else{if(social.following)OutlinedButton(onClick=onFollow,enabled=!social.busy){Text(if(social.busy)"Updating…" else "Following") }else Button(onClick=onFollow,enabled=!social.busy){Text(if(social.busy)"Following…" else "Follow")};RelationshipButton(relationship,onSend,onAccept,onCancel)}};if(!own)TextButton(onClick=onBlock,enabled=!social.busy){Text("Block user",color=MaterialTheme.colorScheme.error)};relationship.error?.let{Text(it,color=MaterialTheme.colorScheme.error)};social.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}}
        item{Text("Posts",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        when{posts.loading->item{Box(Modifier.fillMaxWidth().height(120.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}};posts.error!=null->item{ProfileStateCard(posts.error)};posts.posts.isEmpty()->item{Text("No visible posts yet.",color=MaterialTheme.colorScheme.onSurfaceVariant)};else->items(posts.posts,key=SocialPost::id){ProfilePostCard(it)}}
        profile.created_at?.let{joined->item{Text("Joined ${joined.take(10)}",Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
    }
}

@Composable private fun RelationshipButton(state:ProfileRelationshipState,onSend:()->Unit,onAccept:()->Unit,onCancel:()->Unit){when(state.state){RelationshipState.FRIENDS->OutlinedButton(onClick={},enabled=false){Text("Friends")};RelationshipState.OUTGOING_PENDING->OutlinedButton(onClick=onCancel,enabled=!state.loading){Text(if(state.loading)"Updating…" else "Pending · Cancel")};RelationshipState.INCOMING_PENDING->Button(onClick=onAccept,enabled=!state.loading){Text(if(state.loading)"Accepting…" else "Accept Friend")};RelationshipState.NONE->Button(onClick=onSend,enabled=!state.loading){Text(if(state.loading)"Sending…" else "Add Friend")}}}

@Composable private fun ProfileEditor(profile:AccountProfile,edit:ProfileEditState,onChange:(ProfileEditField,String)->Unit,onSave:()->Unit,onCancel:()->Unit,onAvatar:(ByteArray)->Unit){val context=LocalContext.current;var bitmap by remember{mutableStateOf<Bitmap?>(null)};var zoom by remember{mutableFloatStateOf(1f)};var ox by remember{mutableFloatStateOf(0f)};var oy by remember{mutableFloatStateOf(0f)};var avatarError by remember{mutableStateOf<String?>(null)};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null){val mime=context.contentResolver.getType(uri).orEmpty();val size=runCatching{context.contentResolver.openAssetFileDescriptor(uri,"r")?.use{it.length}?:-1L}.getOrDefault(-1L);when{!mime.startsWith("image/")->avatarError="Please choose an image file.";size>10*1024*1024->avatarError="Avatar must be 10 MB or smaller.";else->runCatching{context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}}.onSuccess{b->if(b==null)avatarError="Unable to read selected image." else{bitmap=b;zoom=1f;ox=0f;oy=0f;avatarError=null}}.onFailure{avatarError=it.message?:"Unable to read selected image."}}}};LazyColumn(Modifier.fillMaxSize(),contentPadding=androidx.compose.foundation.layout.PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Edit Profile",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);AvatarEditor(profile.avatar_url,profile.display_name,bitmap,zoom,ox,oy,launcher,{zoom=it},{ox=it},{oy=it},{b->onAvatar(cropAvatar(b,zoom,ox,oy));bitmap=null},avatarError,edit.saving)};item{ProfileField("Display name",edit.displayName,{onChange(ProfileEditField.DisplayName,it)},true)};item{ProfileField("Bio",edit.bio,{onChange(ProfileEditField.Bio,it)},false,3)};item{ProfileField("Date of birth",edit.dateOfBirth,{onChange(ProfileEditField.DateOfBirth,it)},true)};item{ProfileField("Gender",edit.gender,{onChange(ProfileEditField.Gender,it)},true)};item{ProfileField("Location",edit.location,{onChange(ProfileEditField.Location,it)},true)};item{ProfileField("Website",edit.website,{onChange(ProfileEditField.Website,it)},true)};edit.error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Button(onClick=onSave,enabled=!edit.saving,modifier=Modifier.weight(1f)){if(edit.saving)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Text("Save changes")};OutlinedButton(onClick=onCancel,enabled=!edit.saving,modifier=Modifier.weight(1f)){Text("Cancel")}}}}
}
@Composable private fun AvatarEditor(url:String?,name:String,bitmap:Bitmap?,zoom:Float,ox:Float,oy:Float,launcher:androidx.activity.result.ActivityResultLauncher<String>,onZoom:(Float)->Unit,onOx:(Float)->Unit,onOy:(Float)->Unit,onUse:(Bitmap)->Unit,error:String?,busy:Boolean){Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){if(bitmap!=null){Box(Modifier.size(160.dp).clip(CircleShape)){Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize().graphicsLayer(scaleX=zoom,scaleY=zoom,translationX=ox*.8f,translationY=oy*.8f),contentScale=ContentScale.Crop)};Text("Zoom");Slider(value=zoom,onValueChange=onZoom,valueRange=1f..3f);Text("Horizontal");Slider(value=ox,onValueChange=onOx,valueRange=-100f..100f);Text("Vertical");Slider(value=oy,onValueChange=onOy,valueRange=-100f..100f);Button(onClick={onUse(bitmap)},enabled=!busy){Text("Use this photo")}}else{ProfileAvatar(url,name,112);Button(onClick={launcher.launch("image/*")},enabled=!busy){Text("Choose profile photo")}};Text("JPG, PNG, WebP or GIF · max 10 MB",style=MaterialTheme.typography.labelSmall);error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}}
@Composable private fun ProfileField(label:String,value:String,onChange:(String)->Unit,single:Boolean,min:Int=1){OutlinedTextField(value=value,onValueChange=onChange,modifier=Modifier.fillMaxWidth(),label={Text(label)},singleLine=single,minLines=min)}
@Composable private fun ProfilePostCard(post:SocialPost){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),elevation=CardDefaults.cardElevation(2.dp)){Column(Modifier.padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){ProfileAvatar(post.author.avatar_url,post.author.display_name,44);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(post.author.display_name,fontWeight=FontWeight.SemiBold);Text("@${post.author.username} · ${post.created_at.take(16).replace('T',' ')}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}};if(post.content.isNotBlank())Text(post.content,Modifier.padding(top=10.dp));post.media.forEach{m->if(m.kind=="image")AsyncImage(model=m.public_url,contentDescription=m.file_name,modifier=Modifier.fillMaxWidth().padding(top=10.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.FillWidth)};Text("♥ ${post.likeCount}",Modifier.fillMaxWidth().padding(top=6.dp),textAlign=androidx.compose.ui.text.style.TextAlign.End,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun ProfileAvatar(url:String?,name:String,size:Int){if(!url.isNullOrBlank())AsyncImage(model=url,contentDescription="$name avatar",modifier=Modifier.size(size.dp).clip(CircleShape),contentScale=ContentScale.Crop)else Box(Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=.12f)),contentAlignment=Alignment.Center){Text(name.firstOrNull()?.uppercase()?:"?",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}}
@Composable private fun ProfileStateCard(message:String){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Card(shape=RoundedCornerShape(18.dp)){Text(message,Modifier.padding(20.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun ProfileBlockedCard(busy:Boolean,error:String?,unblock:()->Unit){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("User blocked",fontWeight=FontWeight.Bold);Text("You have blocked this user. Their profile and content are hidden from you.");Button(onClick=unblock,enabled=!busy){Text(if(busy)"Unblocking…" else "Unblock user")};error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}}}
@Composable private fun ProfileErrorCard(message:String,retry:()->Unit){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Unable to load profile",fontWeight=FontWeight.Bold);Text(message,color=MaterialTheme.colorScheme.error);Button(onClick=retry){Text("Try again")}}}}}
private fun cropAvatar(bitmap:Bitmap,zoom:Float,ox:Float,oy:Float):ByteArray{val size=512;val source=minOf(bitmap.width,bitmap.height).toFloat();val scale=(size/source)/zoom.coerceAtLeast(1f);val w=bitmap.width*scale;val h=bitmap.height*scale;val mx=maxOf(0f,(w-size)/2f);val my=maxOf(0f,(h-size)/2f);val x=(size-w)/2f+(ox/100f)*mx;val y=(size-h)/2f+(oy/100f)*my;val out=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas(out).drawBitmap(bitmap,null,RectF(x,y,x+w,y+h),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG));return ByteArrayOutputStream().use{out.compress(Bitmap.CompressFormat.JPEG,90,it);out.recycle();it.toByteArray()}}
