package com.example.meetingroomstatus

import android.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import com.example.meetingroomstatus.data.DBDevice
import com.example.meetingroomstatus.data.DBMeetingList
import com.example.meetingroomstatus.data.MeetingList
import com.example.meetingroomstatus.databinding.ActivityMainBinding
import com.example.meetingroomstatus.network.WoojeonApiService
import com.example.meetingroomstatus.network.networkMng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class MainActivity : AppCompatActivity() {
    private var _binding:ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var uuid = ""
    private var noRoom = ""
    private var nmRoom = ""
    private var dttime = ""
    private lateinit var mTimeHandler : Handler

    private var images : ArrayList<Int> = ArrayList()
    private var arMeetingList = ArrayList<MeetingList>()

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //Get Device's ID
        uuid = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        binding.txtDeviceID.text = "ID : ${uuid}"
        //Set Device info & Get MeetingRoom' NO
        chkDeviceID(uuid)

        images.add(com.example.meetingroomstatus.R.drawable.img01)
        images.add(com.example.meetingroomstatus.R.drawable.img02)
        images.add(com.example.meetingroomstatus.R.drawable.img03)
        images.add(com.example.meetingroomstatus.R.drawable.img04)
        images.add(com.example.meetingroomstatus.R.drawable.img05)
        for (img in images)
            flipperImage(img)
        // run every 10 sec
        mTimeHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                getMeetingRoomInfo(uuid)
                this.sendEmptyMessageDelayed(0, 10000)
            }
        }
        mTimeHandler.sendEmptyMessage(0)
    }

    private fun flipperImage(img: Int) {
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.setImageResource(img)
        binding.imageSlide.addView(imageView)
        binding.imageSlide.flipInterval = 10000
        binding.imageSlide.isAutoStart = true
        binding.imageSlide.setInAnimation(this, android.R.anim.fade_in)
        binding.imageSlide.setOutAnimation(this, android.R.anim.fade_out)

    }

    private fun chkDeviceID(id:String) {
        if (networkMng(this).checkNetworkState()) {
            val api = WoojeonApiService.create()
            api.meetingRoomChk(id).enqueue(object: Callback<DBDevice> {
                override fun onResponse(call: Call<DBDevice>, response: Response<DBDevice>) {
                    response.body()?.results?.let {
                        if (it == "NO") {
                            runOnUiThread {
                                Toast.makeText(baseContext, "기기등록 에러발생", Toast.LENGTH_LONG).show()
                            }
                        }
                        if (it == "OK") {
                            noRoom = response.body()?.status!!
                            nmRoom = response.body()?.nmRoom!!
                            dttime = response.body()?.time!!
                            if (noRoom == "00")
                                runOnUiThread {
                                    Toast.makeText(baseContext, "기기의 회의실을 배정해주세요", Toast.LENGTH_LONG).show()
                                }
                            else
                                runOnUiThread {
                                    binding.txtNmRoom.text = nmRoom
                                    binding.txtDateTime.text = dttime
                                }
                        }
                    }
                }
                override fun onFailure(call: Call<DBDevice>, t: Throwable) {
                    t.printStackTrace()
                }
            })
        }
    }

    private fun getMeetingRoomInfo(id:String) {
        if (networkMng(this).checkNetworkState()){
            arMeetingList.clear()
            val api = WoojeonApiService.create()
            api.meetingRoomList(id).enqueue(object :Callback<DBMeetingList> {
                override fun onResponse(
                    call: Call<DBMeetingList>,
                    response: Response<DBMeetingList>
                ) {
                    arMeetingList.addAll(response.body()?.results!!)
                    setDisplay()
                }
                override fun onFailure(call: Call<DBMeetingList>, t: Throwable) {
                    t.printStackTrace()
                }

            })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setDisplay() {
        var now = "N"
        var next = "N"
        lateinit var nowMeeting :MeetingList
        lateinit var nextMeeting :MeetingList

        for (index in 0 until arMeetingList.size) {
            if (arMeetingList[index].gbn == "TIME")
                binding.txtDateTime.text = arMeetingList[index].dttime
            else if (arMeetingList[index].gbn == "NOW") {
                nowMeeting = arMeetingList[index]
                now = "Y"
            } else if (arMeetingList[index].gbn == "NEXT") {
                nextMeeting = arMeetingList[index]
                next = "Y"
            }
        }
//        if (arMeetingList.size > 0) {
//            if (arMeetingList[0].gbn == "NOW") {
//                nowMeeting = arMeetingList[0]
//                now = "Y"
//            }
//            if (arMeetingList[0].gbn == "NEXT") {
//                nextMeeting = arMeetingList[0]
//                next = "Y"
//            }
//            if (arMeetingList[0].gbn == "TIME") {
//                binding.txtDateTime.text = arMeetingList[0].dttime
//            }
//            if (arMeetingList.size > 1)
//                if (arMeetingList[1].gbn == "NEXT") {
//                    nextMeeting = arMeetingList[1]
//                    next = "Y"
//                }
//        }

        if (now == "N") {
            //is not Meeting on Now
            binding.llNowData.visibility = View.GONE
            binding.txtNowEmpty.visibility = View.VISIBLE
            val constraintLayout = findViewById<ConstraintLayout>(binding.clMeeting.id)
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(
                binding.linearLayout3.id,
                ConstraintSet.TOP,
                binding.txtNowEmpty.id,
                ConstraintSet.BOTTOM,
                0
            )

            constraintSet.applyTo(constraintLayout)
//            val constraintLayout = ConstraintLayout(binding.clMeeting.context)
//            val constraints = ConstraintSet()
//            constraints.clone(constraintLayout)
//            constraints.connect(
//                binding.linearLayout3.id,
//                ConstraintSet.TOP,
//                binding.txtNowEmpty.id,
//                ConstraintSet.BOTTOM,0
//            )
//            constraints.applyTo(constraintLayout)
        }
        else {
            //Meeting on Now
            binding.llNowData.visibility = View.VISIBLE
            binding.txtNowEmpty.visibility = View.GONE
            val constraintLayout = findViewById<ConstraintLayout>(binding.clMeeting.id)
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(
                binding.linearLayout3.id,
                ConstraintSet.TOP,
                binding.llNowData.id,
                ConstraintSet.BOTTOM,
                0
            )
            constraintSet.applyTo(constraintLayout)

            binding.txtNmMeeting.text = nowMeeting.nmMeet
            binding.txtPartMeeting.text = nowMeeting.part
            binding.txtName.text = nowMeeting.kname
            binding.txtTimMeeting.text = "${nowMeeting.frTime} ~ ${nowMeeting.toTime}"

        }
        if (next == "N")
            binding.txtNextTime.text = "일정없음"
        else
            binding.txtNextTime.text = "${nextMeeting.frTime} ~ ${nextMeeting.toTime}"
    }
}