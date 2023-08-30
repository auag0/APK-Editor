package com.anago.apkeditor.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import com.anago.apkeditor.adapters.FileListAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class FileSelectDialog(
    private val title: String,
    private val rootFile: File,
    private val isFolderOnly: Boolean,
    private val callback: Callback
) :
    DialogFragment(),
    FileListAdapter.Callback {
    private lateinit var dialogView: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return dialogView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = layoutInflater.inflate(R.layout.dialog_file_select, null, false)
        return MaterialAlertDialogBuilder(requireContext()).apply {
            setView(dialogView)
        }.create().apply {
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private lateinit var mTitleView: TextView
    private lateinit var mFilePathView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSelect: MaterialButton
    private lateinit var fileListAdapter: FileListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mTitleView = view.findViewById(R.id.title)
        mFilePathView = view.findViewById(R.id.filePath)
        recyclerView = view.findViewById(R.id.recyclerView)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSelect = view.findViewById(R.id.btn_select)

        if (isFolderOnly) {
            btnSelect.visibility = View.VISIBLE
            btnSelect.setOnClickListener {
                callback.onSelectedFile(fileListAdapter.getCurrentDir())
                dismiss()
            }
        } else {
            btnSelect.visibility = View.GONE
        }
        fileListAdapter = FileListAdapter(requireContext(), rootFile, null, this)

        btnCancel.setOnClickListener {
            dismiss()
        }

        recyclerView.adapter = fileListAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        mTitleView.text = title

        fileListAdapter.updateFileList()
        updateFilePathText()
    }

    interface Callback {
        fun onSelectedFile(file: File)
    }

    override fun onFileClicked(file: File) {
        if (file.isFile) {
            if (!isFolderOnly) {
                callback.onSelectedFile(file)
                dismiss()
            }
        } else {
            fileListAdapter.openDirectory(file)
            updateFilePathText()
        }
    }

    private fun updateFilePathText() {
        mFilePathView.text = fileListAdapter.getCurrentDir().absolutePath
    }
}