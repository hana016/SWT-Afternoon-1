package at.tugraz.ist.sw20.swta1.cheat.ui.chat

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.tugraz.ist.sw20.swta1.cheat.ChatActivity
import at.tugraz.ist.sw20.swta1.cheat.R
import at.tugraz.ist.sw20.swta1.cheat.bluetooth.BluetoothService
import at.tugraz.ist.sw20.swta1.cheat.bluetooth.BluetoothState
import kotlinx.android.synthetic.main.chat_fragment.view.*
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.*

class ChatFragment : Fragment() {
    companion object {
        fun newInstance() = ChatFragment()
    }

    private lateinit var viewModel: ChatViewModel
    private var chatEntries = mutableListOf<ChatEntry>() as ArrayList

    private lateinit var root: View
    private lateinit var chatAdapter: ChatHistoryAdapter
    private lateinit var recyclerView: RecyclerView
    
    private val RESULT_SELECT_IMAGE = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        root =  inflater.inflate(R.layout.chat_fragment, container, false)

        val header = root.item_header_text.findViewById<TextView>(R.id.title)
        header.text = BluetoothService.getConnectedDevice()!!.name

        BluetoothService.setOnMessageReceive { chatEntry ->
            chatEntry.isByMe = false
            Log.i("Message", "Message received: ${chatEntry.getMessage()}")
            chatEntries.add(chatEntry)
            activity!!.runOnUiThread {
                chatAdapter.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(chatEntries.size - 1)
            }
        }

        BluetoothService.setOnStateChangeListener { _, newState ->
            val connection_status = root.findViewById<TextView>(R.id.connection_status)

            activity!!.runOnUiThread {
                when (newState) {
                    BluetoothState.CONNECTED -> connection_status.text =
                        getString(R.string.connected_status)
                    BluetoothState.READY -> connection_status.text =
                        getString(R.string.disconnected_status)
                    else -> {
                    }
                }
            }
        }

        chatAdapter = ChatHistoryAdapter(
            chatEntries
        )

        recyclerView = root.findViewById<RecyclerView>(R.id.chat_history).apply {
            layoutManager = LinearLayoutManager(context!!)
            adapter = chatAdapter
        }

        (recyclerView.layoutManager as LinearLayoutManager).stackFromEnd = true

        initSendButton()
        initPictureSendButton()
        initConnectionButton()

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun initConnectionButton() {
        val connection_status = root.findViewById<TextView>(R.id.connection_status)
        connection_status.setOnClickListener {
            if(BluetoothService.state == BluetoothState.CONNECTED) {
                (activity as ChatActivity).disconnect()
            }
        }
    }

    private fun initSendButton() {
        val btnSend = root.item_text_entry_field.findViewById<Button>(R.id.btn_send)
        val etMsg = root.item_text_entry_field.findViewById<EditText>(R.id.text_entry)

        btnSend.setOnClickListener {
            val text = etMsg.text.toString().trim()
            if (BluetoothService.state != BluetoothState.CONNECTED) {
                Toast.makeText(context, "Can't sent message while disconnected.", Toast.LENGTH_SHORT).show()
            }
            else if (text.isNotBlank()) {
                val chatEntry = ChatEntry(text, true, false, Date())
                chatEntries.add(chatEntry)
                BluetoothService.sendMessage(chatEntry)
                chatAdapter.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(chatEntries.size - 1)
                etMsg.text.clear()
            }
        }
    }
    
    private fun initPictureSendButton() {
        val imageBtn = root.item_text_entry_field.findViewById<ImageButton>(R.id.image_select)
    
        imageBtn.setOnClickListener {
            
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivityForResult(intent, RESULT_SELECT_IMAGE)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if(resultCode == RESULT_OK && requestCode == RESULT_SELECT_IMAGE && data != null) {
            if (BluetoothService.state != BluetoothState.CONNECTED) {
                Toast.makeText(context, "Can't sent image while disconnected.", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("Image", "Image selected from gallery")

                val builder = AlertDialog.Builder(context!!)
                builder.setTitle("Send Image")
                builder.setMessage("Do you want to send this image?")

                builder.setPositiveButton("YES") { dialog, which ->

                    val etMsg = root.item_text_entry_field.findViewById<EditText>(R.id.text_entry)
                    val bitMap =
                        MediaStore.Images.Media.getBitmap(context?.contentResolver, data.data!!)
                    val bos = ByteArrayOutputStream()
                    bitMap.compress(Bitmap.CompressFormat.JPEG, 70, bos)
                    val array: ByteArray = bos.toByteArray()
                    bitMap.recycle()
                    Log.d("Image", "Image compressed, size ${array.size}")


                    val chatEntry = ChatEntry("", array, true, false, Date())
                    chatEntries.add(chatEntry)
                    BluetoothService.sendMessage(chatEntry)
                    chatAdapter.notifyDataSetChanged()
                    recyclerView.smoothScrollToPosition(chatEntries.size - 1)
                    etMsg.text.clear()
                }

                builder.setNegativeButton("NO"){_,_ -> }
            }
        }
    }
}