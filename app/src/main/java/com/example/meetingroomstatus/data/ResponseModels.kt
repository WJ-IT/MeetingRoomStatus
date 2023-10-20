package com.example.meetingroomstatus.data

//검색화면: 라이더 검색
data class DBDevice(val results:String?, val status:String?, var nmRoom:String?, var time:String?)

data class DBMeetingList(val results : List<MeetingList>)
data class MeetingList(
    val gbn:String?,
    val nmRoom:String?,
    val dtMeet:String?,
    val frTime:String?,
    val toTime:String?,
    val nmMeet:String?,
    val kname:String?,
    val part:String?,
    val dttime:String?
)