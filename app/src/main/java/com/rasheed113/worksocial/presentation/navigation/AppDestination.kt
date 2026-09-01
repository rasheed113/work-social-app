package com.rasheed113.worksocial.presentation.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Social : AppDestination("social", "Social")
    data object Inbox : AppDestination("inbox", "Inbox")
    data object Friends : AppDestination("friends", "Friends")
    data object Activity : AppDestination("activity", "Activity")
    data object Profile : AppDestination("profile", "Profile")
    data object PublicProfile : AppDestination("profile/{profileId}", "Profile")
    data object CreatePost : AppDestination("social/create-post", "Create Post")
    data object WorkHouse : AppDestination("work-house", "Work House")
}
