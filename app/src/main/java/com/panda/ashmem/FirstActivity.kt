package com.panda.ashmem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.baidu.ryujin.ktc.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import android.system.Os


class FirstActivity : AppCompatActivity() {
    var mBinder: Binder? = null
    var memoryFile: MemoryFile? = null
    var count: Int = 1

    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mBinder = service as Binder
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, ShareMemoryService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }


    fun gotoMain2(view: View) {
        startActivity(Intent(this@FirstActivity, SecondActivity::class.java))
    }

    /**
     * 创建共享内存区域
     */
    fun createShareMemory(view: View) {
        memoryFile = MemoryFile("myShareMemory1", 1024)
        mBinder?.apply {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            val getFileDescriptorMethod: Method =
                MemoryFile::class.java.getDeclaredMethod("getFileDescriptor")
            val fileDescriptor: FileDescriptor = getFileDescriptorMethod.invoke(memoryFile) as FileDescriptor
            val descriptor: Field = FileDescriptor::class.java.getDeclaredField("descriptor")
            descriptor.isAccessible = true
            val id = descriptor.get(fileDescriptor)?:0
            data.writeFileDescriptor(ParcelFileDescriptor.dup(fileDescriptor).fileDescriptor)
            // 通过binder将fd的信息传给另外一个进程
            transact(ShareMemoryService.TRANS_CODE_SET_FD, data, reply, 0)
        }

    }

    /**
     * 将数据写入到共享文件中
     */
    fun write(view: View) {
        memoryFile?.apply {
            val data: ByteArray = "count(${count++})".toByteArray()
            writeBytes(data, 0, 0, data.size)
            btn_write.text = "write($count)"
        }
    }

    /**
     * 读取共享文件
     */
    fun read(view: View) {
        memoryFile?.apply {
            val data = ByteArray(20)
            readBytes(data, 0, 0, data.size)
            tv_msg.text = String(data, charset("utf-8"))
        }
    }

    /**
     * 关闭共享文件
     */
    fun close(view: View) {
        memoryFile?.close()
    }

    override fun onDestroy() {
        unbindService(mConnection)
        memoryFile?.close()
        super.onDestroy()
    }
}
