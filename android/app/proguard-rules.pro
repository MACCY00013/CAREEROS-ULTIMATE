# Keep Room entities and generated database metadata available to Room at runtime.
-keep class com.maccy.careeros.** extends androidx.room.RoomDatabase { *; }
-keep class com.maccy.careeros.SkillEntity { *; }