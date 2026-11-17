# Persisted data: Ussd is Java-serialized into SharedPreferences. Keep its name and members so stored
# entries from earlier versions can still be deserialized (default serialVersionUID depends on the class shape;
# the UID is also pinned in the class, this protects the class/field names from renaming).
-keep class com.tooploox.ussd.domain.Ussd { *; }
-keepnames class com.tooploox.ussd.domain.Ussd
