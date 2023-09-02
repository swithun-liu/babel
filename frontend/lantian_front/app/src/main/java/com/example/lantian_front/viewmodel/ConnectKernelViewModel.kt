package com.example.lantian_front.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.lantian_front.Config
import com.example.lantian_front.SwithunLog
import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.model.*
import com.example.lantian_front.model.MessageTextDTO.OptionCode
import com.example.lantian_front.viewmodel.filemanager.FileManagerViewModel
import com.example.lantian_front.websocket.RawDataBase
import com.example.lantian_front.websocket.RawDataBase.RawTextData
import com.example.lantian_front.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.fs.UsbFile
import okio.ByteString
import java.lang.Exception
import java.nio.ByteBuffer

class ConnectKernelViewModel :
    BaseViewModel<ConnectKernelViewModel.Action, Unit, Unit>() {

    private var remoteWordFlow: Flow<RawDataBase>? = null
    private val repository = WebSocketRepository()
    private var vmCollection: VMCollection? = null

//    @Stable
//    interface UIState {
//    }
//
//    class MutableUIState: UIState {
//    }

    fun init(vmCollection: VMCollection) {
        this.vmCollection = vmCollection
    }


    override fun getInitialUIState(): Unit {
        return Unit
    }

    sealed class Action : BaseViewModel.Action() {
        object ConnectKernelAction : Action()
        class Response2Kernel(val dto: MessageDTO) : Action()

        class ServerGetAndroidUsbFileFileManagerResponse(val usbFile: UsbFile?, val uuid: String): Action()

        class ServerGetAndroidUsbFileByPieceFileManagerResponse(
            val buffer: ByteBuffer,
            val uuid: String,
            val pos: String
        ) : Action()
    }

    override fun reduce(action: Action) {
        when (action) {
            Action.ConnectKernelAction -> connectKernel()
            is Action.Response2Kernel -> response2Kernel(action)
            is Action.ServerGetAndroidUsbFileFileManagerResponse -> handleReceiveServerGetAndroidUsbFileSizeFileManagerViewModelResponse(action)
            is Action.ServerGetAndroidUsbFileByPieceFileManagerResponse -> handleReceiveServerGetAndroidUsbFileByPieceResponse(action)
        }
    }

    private fun connectKernel() {
        val kernelConfig = Config.kernelConfig

        remoteWordFlow = repository.webSocketCreate(
            "http://${kernelConfig.kernelHost}/${KernelConfig.KernelPath.ConnectPath.connect}",
            viewModelScope,
            "Connect"
        )
        viewModelScope.launch(Dispatchers.IO) {
            remoteWordFlow?.collect {
                when (it) {
                    is RawDataBase.RawByteData -> {

                    }
                    is RawTextData -> {
                        val json = it.json
                        val gson = Gson()
                        try {
                            SwithunLog.d("from kernel: $json")
                            val jsonObject = gson.fromJson(json, MessageTextDTO::class.java)
                            SwithunLog.d("get kernal code: ${jsonObject.code}, ${jsonObject.uuid}, ${jsonObject.content}")
                            handleResponse(jsonObject)
                        } catch (e: Exception) {
                            SwithunLog.d("parse err")
                        }

                    }
                }
            }
        }
    }

    private fun response2Kernel(action: Action.Response2Kernel) {
        when (val dto = action.dto) {
            is MessageBinaryDTO -> {

            }
            is MessageTextDTO -> {
                webSocketSend(RawTextData(dto.toJsonStr()))
            }
        }
    }

    private fun webSocketSend(data: RawTextData) {
        repository.webSocketSend(data)
    }


    private fun handleResponse(data: MessageTextDTO) {
        when (OptionCode.fromValue(data.code)) {
            OptionCode.GET_BASE_PATH_LIST_REQUEST -> {
                vmCollection?.let {
                    it.fileVM.viewModelScope.launch(Dispatchers.IO) {
                        val basePathList = it.fileVM.getBasePathListFromLocal()
                        val gson = Gson()
                        val pathListJsonStr = gson.toJson(basePathList)
                        val dto = MessageTextDTO(
                            data.uuid,
                            OptionCode.GET_BASE_PATH_LIST_RESPONSE.code,
                            pathListJsonStr,
                            MessageTextDTO.ContentType.TEXT.type
                        )
                        reduce(Action.Response2Kernel(dto))
                    }
                }
            }
            OptionCode.GET_CHILDREN_PATH_LIST_REQUEST -> {
                vmCollection?.let {
                    it.fileVM.viewModelScope.launch(Dispatchers.IO) {
                        val childrenPathList = it.fileVM.getChildrenPathListFromLocal(data.content)
                        val gson = Gson()
                        val pathListJsonStr = gson.toJson(childrenPathList)
                        val dto = MessageTextDTO(
                            data.uuid,
                            OptionCode.GET_BASE_PATH_LIST_RESPONSE.code,
                            pathListJsonStr,
                            MessageTextDTO.ContentType.TEXT.type
                        )
                        reduce(Action.Response2Kernel(dto))
                    }
                }
            }
            OptionCode.ServerGetAndroidUsbFileSize -> handleReceiveServerGetAndroidUsbFileSize(data)
            OptionCode.ServerGetAndroidUsbFileByPiece -> handleReceiveServerGetAndroidUsbFileByPiece(data)
            null -> {
                SwithunLog.d("unKnown code")
            }
            else -> {
                SwithunLog.d("other code")
            }
        }
    }

    private fun handleReceiveServerGetAndroidUsbFileByPiece(data: MessageTextDTO) {
        vmCollection?.fileVM?.reduce(FileManagerViewModel.Action.GetUsbFileByPiece(data))
    }

    private fun handleReceiveServerGetAndroidUsbFileByPieceResponse(action: Action.ServerGetAndroidUsbFileByPieceFileManagerResponse) {
        SwithunLog.d("handleReceiveServerGetAndroidUsbFileByPieceResponse ${action.buffer}")

        try {
            val message = MessageBinaryDTO(
                action.uuid,
                0,
                ByteString.of(*action.buffer.array())
            )

            SwithunLog.d("handleReceiveServerGetAndroidUsbFileByPieceResponse message ${message}")

            viewModelScope.launch {
                SwithunLog.d("handleReceiveServerGetAndroidUsbFileByPieceResponse send")
                repository.webSocketSuspendSend(
                    RawDataBase.RawByteData(
                        ByteString.of(*message.toByteArray())
                    )
                )
            }
        } catch (e: Exception) {
            SwithunLog.e("handleReceiveServerGetAndroidUsbFileByPieceResponse err ${e}")
        }

    }


    private fun handleReceiveServerGetAndroidUsbFileSize(message: MessageTextDTO) {
        val path = message.content
        val vmCollection = vmCollection ?: return
        vmCollection.fileVM.reduce(FileManagerViewModel.Action.FindUsbFile(path, message.uuid))

//        viewModelScope.launch {
//            val dto = MessageTextDTO(
//                message.uuid,
//                OptionCode.ServerGetAndroidUsbFileSize.code,
//                58351478.toString(),
//                MessageTextDTO.ContentType.TEXT.type
//            )
//            reduce(Action.Response2Kernel(dto))
//        }
    }


    private fun handleReceiveServerGetAndroidUsbFileSizeFileManagerViewModelResponse(action: Action.ServerGetAndroidUsbFileFileManagerResponse) {
        SwithunLog.d(" handleReceiveServerGetAndroidUsbFileSizeFileManagerViewModelResponse")
        SwithunLog.d(" handleReceiveServerGetAndroidUsbFileSizeFileManagerViewModelResponse 1 ${action.usbFile}")
        val usbFile = action.usbFile
        SwithunLog.d(" handleReceiveServerGetAndroidUsbFileSizeFileManagerViewModelResponse 2")
        val vmCollection = vmCollection ?: return
        vmCollection.fileVM.viewModelScope.launch(Dispatchers.IO) {
            val size = usbFile?.length ?: 0
            val dto = MessageTextDTO(
                action.uuid,
                OptionCode.ServerGetAndroidUsbFileSize.code,
                size.toString(),
                MessageTextDTO.ContentType.TEXT.type
            )
            reduce(Action.Response2Kernel(dto))
        }
    }


}