package com.panda.ashmem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.view.View
import com.baidu.ryujin.ktc.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException

class SecondActivity : AppCompatActivity() {
    var mBinder: IBinder? = null
    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mBinder = service
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val intent = Intent(this, ShareMemoryService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * 另外一个进程读取共享文件内容
     */
    fun read(view: View) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        mBinder?.apply {
            transact(ShareMemoryService.TRANS_CODE_GET_FD, data, reply, 0)
            var fi: FileInputStream? = null
            var fileDescriptor: FileDescriptor? = null
            val data = ByteArray(20)
            try {
                val pfd = reply.readFileDescriptor() ?: return
                fileDescriptor = pfd.fileDescriptor
                fi = FileInputStream(fileDescriptor)
                //读取数据
                fi.read(data)
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                if (fileDescriptor != null) {
                    try {
                        fi?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            tv_msg.text = String(data, charset("utf-8"))
        }
    }

    override fun onDestroy() {
        unbindService(mConnection)
        super.onDestroy()
    }
}