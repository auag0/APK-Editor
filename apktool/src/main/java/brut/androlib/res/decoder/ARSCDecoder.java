/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.decoder;

import android.util.TypedValue;

import com.google.common.io.LittleEndianDataInputStream;

import org.apache.commons.io.input.CountingInputStream;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResID;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResType;
import brut.androlib.res.data.ResTypeSpec;
import brut.androlib.res.data.arsc.ARSCData;
import brut.androlib.res.data.arsc.ARSCHeader;
import brut.androlib.res.data.arsc.EntryData;
import brut.androlib.res.data.arsc.FlagsOffset;
import brut.androlib.res.data.value.ResBagValue;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResIntBasedValue;
import brut.androlib.res.data.value.ResReferenceValue;
import brut.androlib.res.data.value.ResScalarValue;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.data.value.ResValueFactory;
import brut.util.Duo;
import brut.util.ExtDataInput;

public class ARSCDecoder {
    private final static short ENTRY_FLAG_COMPLEX = 0x0001;
    private final static short ENTRY_FLAG_PUBLIC = 0x0002;
    private final static short ENTRY_FLAG_WEAK = 0x0004;
    private static final int KNOWN_CONFIG_BYTES = 64;
    private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class.getName());
    private final ExtDataInput mIn;
    private final ResTable mResTable;
    private final CountingInputStream mCountIn;
    private final List<FlagsOffset> mFlagsOffsets;
    private final boolean mKeepBroken;
    private final HashMap<Integer, ResTypeSpec> mResTypeSpecs = new HashMap<>();
    private ARSCHeader mHeader;
    private StringBlock mTableStrings;
    private StringBlock mTypeNames;
    private StringBlock mSpecNames;
    private ResPackage mPkg;
    private ResTypeSpec mTypeSpec;
    private ResType mType;
    private int mResId;
    private int mTypeIdOffset = 0;
    private HashMap<Integer, Boolean> mMissingResSpecMap;

    private ARSCDecoder(InputStream arscStream, ResTable resTable, boolean storeFlagsOffsets, boolean keepBroken) {
        arscStream = mCountIn = new CountingInputStream(arscStream);
        if (storeFlagsOffsets) {
            mFlagsOffsets = new ArrayList<>();
        } else {
            mFlagsOffsets = null;
        }
        // We need to explicitly cast to DataInput as otherwise the constructor is ambiguous.
        // We choose DataInput instead of InputStream as ExtDataInput wraps an InputStream in
        // a DataInputStream which is big-endian and ignores the little-endian behavior.
        mIn = new ExtDataInput((DataInput) new LittleEndianDataInputStream(arscStream));
        mResTable = resTable;
        mKeepBroken = keepBroken;
    }

    public static ARSCData decode(InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken)
            throws AndrolibException {
        return decode(arscStream, findFlagsOffsets, keepBroken, new ResTable());
    }

    public static ARSCData decode(InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken,
                                  ResTable resTable)
            throws AndrolibException {
        try {
            ARSCDecoder decoder = new ARSCDecoder(arscStream, resTable, findFlagsOffsets, keepBroken);
            ResPackage[] pkgs = decoder.readResourceTable();
            return new ARSCData(pkgs, decoder.mFlagsOffsets == null
                    ? null
                    : decoder.mFlagsOffsets.toArray(new FlagsOffset[0]));
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode arsc file", ex);
        }
    }

    private ResPackage[] readResourceTable() throws IOException, AndrolibException {
        Set<ResPackage> pkgs = new LinkedHashSet<>();
        ResTypeSpec typeSpec;

        chunkLoop:
        for (; ; ) {
            nextChunk();

            switch (mHeader.type) {
                case ARSCHeader.RES_NULL_TYPE:
                    readUnknownChunk();
                    break;
                case ARSCHeader.RES_STRING_POOL_TYPE:
                    readStringPoolChunk();
                    break;
                case ARSCHeader.RES_TABLE_TYPE:
                    readTableChunk();
                    break;

                // Chunk types in RES_TABLE_TYPE
                case ARSCHeader.XML_TYPE_PACKAGE:
                    mTypeIdOffset = 0;
                    pkgs.add(readTablePackage());
                    break;
                case ARSCHeader.XML_TYPE_TYPE:
                    readTableType();
                    break;
                case ARSCHeader.XML_TYPE_SPEC_TYPE:
                    typeSpec = readTableSpecType();
                    addTypeSpec(typeSpec);
                    break;
                case ARSCHeader.XML_TYPE_LIBRARY:
                    readLibraryType();
                    break;
                case ARSCHeader.XML_TYPE_OVERLAY:
                    readOverlaySpec();
                    break;
                case ARSCHeader.XML_TYPE_OVERLAY_POLICY:
                    readOverlayPolicySpec();
                    break;
                case ARSCHeader.XML_TYPE_STAGED_ALIAS:
                    readStagedAliasSpec();
                    break;
                default:
                    if (mHeader.type != ARSCHeader.RES_NONE_TYPE) {
                        LOGGER.severe(String.format("Unknown chunk type: %04x", mHeader.type));
                    }
                    break chunkLoop;
            }
        }

        if (mPkg.getResSpecCount() > 0) {
            addMissingResSpecs();
        }

        // We've detected sparse resources, lets record this so we can rebuild in that same format (sparse/not)
        // with aapt2. aapt1 will ignore this.
        if (!mResTable.getSparseResources()) {
            mResTable.setSparseResources(true);
        }

        return pkgs.toArray(new ResPackage[0]);
    }

    private void readStringPoolChunk() throws IOException, AndrolibException {
        checkChunkType(ARSCHeader.RES_STRING_POOL_TYPE);
        mTableStrings = StringBlock.readWithoutChunk(mIn, mHeader.chunkSize);
    }

    private void readTableChunk() throws IOException, AndrolibException {
        checkChunkType(ARSCHeader.RES_TABLE_TYPE);
        mIn.skipInt(); // packageCount
    }

    private void readUnknownChunk() throws IOException, AndrolibException {
        checkChunkType(ARSCHeader.RES_NULL_TYPE);

        LOGGER.warning("Skipping unknown chunk data of size " + mHeader.chunkSize);
        mHeader.skipChunk(mIn);
    }

    private ResPackage readTablePackage() throws IOException, AndrolibException {
        checkChunkType(ARSCHeader.XML_TYPE_PACKAGE);
        int id = mIn.readInt();

        if (id == 0) {
            // This means we are dealing with a Library Package, we should just temporarily
            // set the packageId to the next available id . This will be set at runtime regardless, but
            // for Apktool's use we need a non-zero packageId.
            // AOSP indicates 0x02 is next, as 0x01 is system and 0x7F is private.
            id = 2;
            if (mResTable.getPackageOriginal() == null && mResTable.getPackageRenamed() == null) {
                mResTable.setSharedLibrary(true);
            }
        }

        String name = mIn.readNullEndedString(128, true);
        mIn.skipInt(); // typeStrings
        mIn.skipInt(); // lastPublicType
        mIn.skipInt(); // keyStrings
        mIn.skipInt(); // lastPublicKey

        // TypeIdOffset was added platform_frameworks_base/@f90f2f8dc36e7243b85e0b6a7fd5a590893c827e
        // which is only in split/new applications.
        int splitHeaderSize = (2 + 2 + 4 + 4 + (2 * 128) + (4 * 5)); // short, short, int, int, char[128], int * 4
        if (mHeader.headerSize == splitHeaderSize) {
            mTypeIdOffset = mIn.readInt();
        }

        if (mTypeIdOffset > 0) {
            LOGGER.warning("Please report this application to Apktool for a fix: https://github.com/iBotPeaches/Apktool/issues/1728");
        }

        mTypeNames = StringBlock.readWithChunk(mIn);
        mSpecNames = StringBlock.readWithChunk(mIn);

        mResId = id << 24;
        mPkg = new ResPackage(mResTable, id, name);

        return mPkg;
    }

    private void readLibraryType() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.XML_TYPE_LIBRARY);
        int libraryCount = mIn.readInt();

        int packageId;
        String packageName;

        for (int i = 0; i < libraryCount; i++) {
            packageId = mIn.readInt();
            packageName = mIn.readNullEndedString(128, true);
            LOGGER.info(String.format("Decoding Shared Library (%s), pkgId: %d", packageName, packageId));
        }
    }

    private void readStagedAliasSpec() throws IOException {
        int count = mIn.readInt();

        for (int i = 0; i < count; i++) {
            LOGGER.fine(String.format("Skipping staged alias stagedId (%h) finalId: %h", mIn.readInt(), mIn.readInt()));
        }
    }

    private void readOverlaySpec() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.XML_TYPE_OVERLAY);
        String name = mIn.readNullEndedString(256, true);
        String actor = mIn.readNullEndedString(256, true);
        LOGGER.fine(String.format("Overlay name: \"%s\", actor: \"%s\")", name, actor));
    }

    private void readOverlayPolicySpec() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.XML_TYPE_OVERLAY_POLICY);
        mIn.skipInt(); // policyFlags
        int count = mIn.readInt();

        for (int i = 0; i < count; i++) {
            LOGGER.fine(String.format("Skipping overlay (%h)", mIn.readInt()));
        }
    }

    private ResTypeSpec readTableSpecType() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.XML_TYPE_SPEC_TYPE);
        int id = mIn.readUnsignedByte();
        mIn.skipBytes(1); // reserved0
        mIn.skipBytes(2); // reserved1
        int entryCount = mIn.readInt();

        if (mFlagsOffsets != null) {
            mFlagsOffsets.add(new FlagsOffset(mCountIn.getCount(), entryCount));
        }

        mIn.skipBytes(entryCount * 4); // flags
        mTypeSpec = new ResTypeSpec(mTypeNames.getString(id - 1), mResTable, mPkg, id, entryCount);
        mPkg.addType(mTypeSpec);

        return mTypeSpec;
    }

    private ResType readTableType() throws IOException, AndrolibException {
        checkChunkType(ARSCHeader.XML_TYPE_TYPE);
        int typeId = mIn.readUnsignedByte() - mTypeIdOffset;
        if (mResTypeSpecs.containsKey(typeId)) {
            mResId = (0xff000000 & mResId) | mResTypeSpecs.get(typeId).getId() << 16;
            mTypeSpec = mResTypeSpecs.get(typeId);
        }

        int typeFlags = mIn.readByte();
        mIn.skipBytes(2); // reserved
        int entryCount = mIn.readInt();
        int entriesStart = mIn.readInt();
        mMissingResSpecMap = new LinkedHashMap<>();

        ResConfigFlags flags = readConfigFlags();
        int position = (mHeader.startPosition + entriesStart) - (entryCount * 4);

        // For some APKs there is a disconnect between the reported size of Configs
        // If we find a mismatch skip those bytes.
        if (position != mCountIn.getCount()) {
            LOGGER.warning("Invalid data detected. Skipping: " + (position - mCountIn.getCount()) + " byte(s)");
            mIn.skipBytes(position - mCountIn.getCount());
        }

        if ((typeFlags & 0x01) != 0) {
            LOGGER.fine("Sparse type flags detected: " + mTypeSpec.getName());
        }

        HashMap<Integer, Integer> entryOffsetMap = new LinkedHashMap<>();
        for (int i = 0; i < entryCount; i++) {
            if ((typeFlags & 0x01) != 0) {
                entryOffsetMap.put(mIn.readUnsignedShort(), mIn.readUnsignedShort());
            } else {
                entryOffsetMap.put(i, mIn.readInt());
            }
        }

        if (flags.isInvalid) {
            String resName = mTypeSpec.getName() + flags.getQualifiers();
            if (mKeepBroken) {
                LOGGER.warning("Invalid config flags detected: " + resName);
            } else {
                LOGGER.warning("Invalid config flags detected. Dropping resources: " + resName);
            }
        }

        mType = flags.isInvalid && !mKeepBroken ? null : mPkg.getOrCreateConfig(flags);

        for (int i : entryOffsetMap.keySet()) {
            int offset = entryOffsetMap.get(i);
            if (offset == -1) {
                continue;
            }
            mMissingResSpecMap.put(i, false);
            mResId = (mResId & 0xffff0000) | i;

            // As seen in some recent APKs - there are more entries reported than can fit in the chunk.
            if (mCountIn.getCount() == mHeader.endPosition) {
                int remainingEntries = entryCount - i;
                LOGGER.warning(String.format("End of chunk hit. Skipping remaining entries (%d) in type: %s",
                        remainingEntries, mTypeSpec.getName())
                );
                break;
            }

            readEntry(readEntryData());
        }

        // skip "TYPE 8 chunks" and/or padding data at the end of this chunk
        if (mCountIn.getCount() < mHeader.endPosition) {
            long bytesSkipped = mCountIn.skip(mHeader.endPosition - mCountIn.getCount());
            LOGGER.warning("Unknown data detected. Skipping: " + bytesSkipped + " byte(s)");
        }

        return mType;
    }

    private EntryData readEntryData() throws IOException, AndrolibException {
        short size = mIn.readShort();
        if (size < 0) {
            throw new AndrolibException("Entry size is under 0 bytes.");
        }

        short flags = mIn.readShort();
        int specNamesId = mIn.readInt();
        ResValue value = (flags & ENTRY_FLAG_COMPLEX) == 0 ? readValue() : readComplexEntry();
        EntryData entryData = new EntryData();
        entryData.mFlags = flags;
        entryData.mSpecNamesId = specNamesId;
        entryData.mValue = value;
        return entryData;
    }

    private void readEntry(EntryData entryData) throws AndrolibException {
        int specNamesId = entryData.mSpecNamesId;
        ResValue value = entryData.mValue;

        if (mTypeSpec.isString() && value instanceof ResFileValue) {
            value = new ResStringValue(value.toString(), ((ResFileValue) value).getRawIntValue());
        }
        if (mType == null) {
            return;
        }

        ResID resId = new ResID(mResId);
        ResResSpec spec;
        if (mPkg.hasResSpec(resId)) {
            spec = mPkg.getResSpec(resId);

            if (spec.isDummyResSpec()) {
                removeResSpec(spec);

                spec = new ResResSpec(resId, mSpecNames.getString(specNamesId), mPkg, mTypeSpec);
                mPkg.addResSpec(spec);
                mTypeSpec.addResSpec(spec);
            }
        } else {
            spec = new ResResSpec(resId, mSpecNames.getString(specNamesId), mPkg, mTypeSpec);
            mPkg.addResSpec(spec);
            mTypeSpec.addResSpec(spec);
        }
        ResResource res = new ResResource(mType, spec, value);

        try {
            mType.addResource(res);
            spec.addResource(res);
        } catch (AndrolibException ex) {
            if (mKeepBroken) {
                mType.addResource(res, true);
                spec.addResource(res, true);
                LOGGER.warning(String.format("Duplicate Resource Detected. Ignoring duplicate: %s", res));
            } else {
                throw ex;
            }
        }
    }

    private ResBagValue readComplexEntry() throws IOException, AndrolibException {
        int parent = mIn.readInt();
        int count = mIn.readInt();

        ResValueFactory factory = mPkg.getValueFactory();
        Duo<Integer, ResScalarValue>[] items = new Duo[count];
        ResIntBasedValue resValue;
        int resId;

        for (int i = 0; i < count; i++) {
            resId = mIn.readInt();
            resValue = readValue();

            if (!(resValue instanceof ResScalarValue)) {
                resValue = new ResStringValue(resValue.toString(), resValue.getRawIntValue());
            }
            items[i] = new Duo<>(resId, (ResScalarValue) resValue);
        }

        return factory.bagFactory(parent, items, mTypeSpec);
    }

    private ResIntBasedValue readValue() throws IOException, AndrolibException {
        mIn.skipCheckShort((short) 8); // size
        mIn.skipCheckByte((byte) 0); // zero
        byte type = mIn.readByte();
        int data = mIn.readInt();

        return type == TypedValue.TYPE_STRING
                ? mPkg.getValueFactory().factory(mTableStrings.getHTML(data), data)
                : mPkg.getValueFactory().factory(type, data, null);
    }

    private ResConfigFlags readConfigFlags() throws IOException, AndrolibException {
        int size = mIn.readInt();
        int read = 8;

        if (size < 8) {
            throw new AndrolibException("Config size < 8");
        }

        boolean isInvalid = false;

        short mcc = mIn.readShort();
        short mnc = mIn.readShort();

        char[] language = new char[0];
        char[] country = new char[0];
        if (size >= 12) {
            language = this.unpackLanguageOrRegion(mIn.readByte(), mIn.readByte(), 'a');
            country = this.unpackLanguageOrRegion(mIn.readByte(), mIn.readByte(), '0');
            read = 12;
        }

        byte orientation = 0;
        byte touchscreen = 0;
        if (size >= 14) {
            orientation = mIn.readByte();
            touchscreen = mIn.readByte();
            read = 14;
        }

        int density = 0;
        if (size >= 16) {
            density = mIn.readUnsignedShort();
            read = 16;
        }

        byte keyboard = 0;
        byte navigation = 0;
        byte inputFlags = 0;
        if (size >= 20) {
            keyboard = mIn.readByte();
            navigation = mIn.readByte();
            inputFlags = mIn.readByte();
            mIn.skipBytes(1); // inputPad0
            read = 20;
        }

        short screenWidth = 0;
        short screenHeight = 0;
        short sdkVersion = 0;
        if (size >= 28) {
            screenWidth = mIn.readShort();
            screenHeight = mIn.readShort();

            sdkVersion = mIn.readShort();
            mIn.skipBytes(2); // minorVersion
            read = 28;
        }

        byte screenLayout = 0;
        byte uiMode = 0;
        short smallestScreenWidthDp = 0;
        if (size >= 32) {
            screenLayout = mIn.readByte();
            uiMode = mIn.readByte();
            smallestScreenWidthDp = mIn.readShort();
            read = 32;
        }

        short screenWidthDp = 0;
        short screenHeightDp = 0;
        if (size >= 36) {
            screenWidthDp = mIn.readShort();
            screenHeightDp = mIn.readShort();
            read = 36;
        }

        char[] localeScript = null;
        char[] localeVariant = null;
        if (size >= 48) {
            localeScript = readScriptOrVariantChar(4).toCharArray();
            localeVariant = readScriptOrVariantChar(8).toCharArray();
            read = 48;
        }

        byte screenLayout2 = 0;
        byte colorMode = 0;
        if (size >= 52) {
            screenLayout2 = mIn.readByte();
            colorMode = mIn.readByte();
            mIn.skipBytes(2); // screenConfigPad2
            read = 52;
        }

        if (size > 52) {
            int length = size - read;
            mIn.skipBytes(length); // localeNumberingSystem
            read += length;
        }

        int exceedingSize = size - KNOWN_CONFIG_BYTES;
        if (exceedingSize > 0) {
            byte[] buf = new byte[exceedingSize];
            read += exceedingSize;
            mIn.readFully(buf);
            BigInteger exceedingBI = new BigInteger(1, buf);

            if (exceedingBI.equals(BigInteger.ZERO)) {
                LOGGER.fine(String
                        .format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.",
                                KNOWN_CONFIG_BYTES));
            } else {
                LOGGER.warning(String.format("Config flags size > %d. Size = %d. Exceeding bytes: 0x%X.",
                        KNOWN_CONFIG_BYTES, size, exceedingBI));
                isInvalid = true;
            }
        }

        int remainingSize = size - read;
        if (remainingSize > 0) {
            mIn.skipBytes(remainingSize);
        }

        return new ResConfigFlags(mcc, mnc, language, country,
                orientation, touchscreen, density, keyboard, navigation,
                inputFlags, screenWidth, screenHeight, sdkVersion,
                screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp,
                screenHeightDp, localeScript, localeVariant, screenLayout2,
                colorMode, isInvalid, size);
    }

    private char[] unpackLanguageOrRegion(byte in0, byte in1, char base) {
        // check high bit, if so we have a packed 3 letter code
        if (((in0 >> 7) & 1) == 1) {
            int first = in1 & 0x1F;
            int second = ((in1 & 0xE0) >> 5) + ((in0 & 0x03) << 3);
            int third = (in0 & 0x7C) >> 2;

            // since this function handles languages & regions, we add the value(s) to the base char
            // which is usually 'a' or '0' depending on language or region.
            return new char[]{(char) (first + base), (char) (second + base), (char) (third + base)};
        }
        return new char[]{(char) in0, (char) in1};
    }

    private String readScriptOrVariantChar(int length) throws IOException {
        StringBuilder string = new StringBuilder(16);

        while (length-- != 0) {
            short ch = mIn.readByte();
            if (ch == 0) {
                break;
            }
            string.append((char) ch);
        }
        mIn.skipBytes(length);

        return string.toString();
    }

    private void addTypeSpec(ResTypeSpec resTypeSpec) {
        mResTypeSpecs.put(resTypeSpec.getId(), resTypeSpec);
    }

    private void addMissingResSpecs() throws AndrolibException {
        int resId = mResId & 0xffff0000;

        for (int i : mMissingResSpecMap.keySet()) {
            if (mMissingResSpecMap.get(i)) continue;

            ResResSpec spec = new ResResSpec(new ResID(resId | i), "APKTOOL_DUMMY_" + Integer.toHexString(i), mPkg, mTypeSpec);

            // If we already have this resID don't add it again.
            if (!mPkg.hasResSpec(new ResID(resId | i))) {
                mPkg.addResSpec(spec);
                mTypeSpec.addResSpec(spec);

                if (mType == null) {
                    mType = mPkg.getOrCreateConfig(new ResConfigFlags());
                }

                // We are going to make dummy attributes a null reference (@null) now instead of a boolean false.
                // This is because aapt2 is much more strict when it comes to what we can put in an application.
                ResValue value = new ResReferenceValue(mPkg, 0, "");

                ResResource res = new ResResource(mType, spec, value);
                mType.addResource(res);
                spec.addResource(res);
            }
        }
    }

    private void removeResSpec(ResResSpec spec) {
        if (mPkg.hasResSpec(spec.getId())) {
            mPkg.removeResSpec(spec);
            mTypeSpec.removeResSpec(spec);
        }
    }

    private ARSCHeader nextChunk() throws IOException {
        return mHeader = ARSCHeader.read(mIn, mCountIn);
    }

    private void checkChunkType(int expectedType) throws AndrolibException {
        if (mHeader.type != expectedType) {
            throw new AndrolibException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x",
                    expectedType, mHeader.type));
        }
    }
}
