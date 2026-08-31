package com.rasheed113.worksocial

import android.app.Application
import com.rasheed113.worksocial.infrastructure.supabase.SupabaseClientProvider

class WorkSocialApplication : Application() {
    val supabase by lazy { SupabaseClientProvider.create() }
}
