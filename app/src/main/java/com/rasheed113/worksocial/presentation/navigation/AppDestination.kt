package com.rasheed113.worksocial.presentation.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Social : AppDestination("social", "Social")
    data object Activity : AppDestination("activity", "Activity")
    data object CreatePost : AppDestination("social/create-post", "Create Post")
    data object WorkHouse : AppDestination("work-house", "Work House")
}
