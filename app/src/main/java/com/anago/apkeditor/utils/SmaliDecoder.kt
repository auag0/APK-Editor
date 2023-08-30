package com.anago.apkeditor.utils

import com.android.tools.smali.baksmali.Baksmali
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.analysis.InlineMethodResolver
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import java.io.File

class SmaliDecoder private constructor(
    private val mApkFile: File,
    private val mOutDir: File,
    private val mDexFile: String,
    private val mDebugMode: Boolean,
    private val mApiLevel: Int
) {
    private fun decode(): DexFile {
        val options = BaksmaliOptions().apply {
            deodex = false
            implicitReferences = false
            parameterRegisters = true
            localsDirective = true
            sequentialLabels = true
            debugInfo = mDebugMode
            codeOffsets = false
            accessorComments = false
            registerInfo = 0
            inlineResolver = null
        }

        var jobs = Runtime.getRuntime().availableProcessors()
        if (jobs > 6) {
            jobs = 6
        }

        val container = DexFileFactory.loadDexContainer(
            mApkFile,
            if (mApiLevel > 0) Opcodes.forApi(mApiLevel) else null
        )
        var dexEntry: MultiDexContainer.DexEntry<out DexBackedDexFile>?

        dexEntry = if (container.dexEntryNames.size == 1) {
            container.getEntry(container.dexEntryNames[0])
        } else {
            container.getEntry(mDexFile)
        }

        // Double-check the passed param exists
        if (dexEntry == null) {
            dexEntry = container.getEntry(container.dexEntryNames[0])
        }
        assert(dexEntry != null)
        val dexFile: DexBackedDexFile = dexEntry!!.dexFile
        if (dexFile.supportsOptimizedOpcodes()) {
            throw Exception("Warning: You are disassembling an odex file without deodexing it.")
        }
        if (dexFile is DexBackedOdexFile) {
            options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(dexFile.odexVersion)
        }
        Baksmali.disassembleDexFile(dexFile, mOutDir, jobs, options)
        return dexFile
    }

    companion object {
        fun decode(
            apkFile: File,
            outDir: File,
            dexName: String,
            debugMode: Boolean,
            apiLevel: Int
        ): DexFile {
            return SmaliDecoder(apkFile, outDir, dexName, debugMode, apiLevel).decode()
        }
    }
}
