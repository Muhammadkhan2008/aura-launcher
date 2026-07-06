package com.aura.launcher

import android.graphics.drawable.ColorDrawable

/** Test helper — icon Drawable ke saath ek AppInfo banata hai. */
fun testAppInfo(
    label: String,
    packageName: String = "com.$label",
    activityName: String = "$packageName.Main"
): AppInfo = AppInfo(
    label = label,
    packageName = packageName,
    activityName = activityName,
    icon = ColorDrawable(0)
)
