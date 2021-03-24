package com.example.matageek.fruity.types

import junit.framework.TestCase

import org.junit.Test

class PrimitiveTypesTest : TestCase() {
    @Test
    fun test_ModuleIdWrapper() {
        val moduleIdWrapperFromVendorId =
            ModuleIdWrapper(ModuleId.VENDOR_MODULE_ID_PREFIX.id, 1, 0xAB24.toShort())
        // VENDOR_MODULE_ID_PREFIX = 0xF0
        assert(moduleIdWrapperFromVendorId.wrappedModuleId == 0xAB2401F0.toInt())
        val moduleIdWrapperFromPrimaryModuleId =
            ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id)
        assert(moduleIdWrapperFromPrimaryModuleId.prefix == ModuleId.STATUS_REPORTER_MODULE.id)
        assert(moduleIdWrapperFromPrimaryModuleId.subId == ModuleIdWrapper.SUB_ID_FOR_PRIMARY_MODULE_ID)
        assert(moduleIdWrapperFromPrimaryModuleId.vendorId == ModuleIdWrapper.VENDOR_ID_FOR_PRIMARY_MODULE_ID)
        assert(moduleIdWrapperFromPrimaryModuleId.wrappedModuleId == 0xFFFFFF03.toInt())
        val moduleIdWrapperFromWrappedId =
            ModuleIdWrapper(0xAB2401F0.toInt())
        assert(moduleIdWrapperFromWrappedId.prefix == ModuleId.VENDOR_MODULE_ID_PREFIX.id)
        assert(moduleIdWrapperFromWrappedId.subId == 1.toByte())
        assert(moduleIdWrapperFromWrappedId.vendorId == 0xAB24.toShort())
        val _moduleIdWrapperFromWrappedId =
            ModuleIdWrapper(0xFFFFFF03.toInt())
        assert(_moduleIdWrapperFromWrappedId.prefix == ModuleId.STATUS_REPORTER_MODULE.id)
        assert(_moduleIdWrapperFromWrappedId.subId == ModuleIdWrapper.SUB_ID_FOR_PRIMARY_MODULE_ID)
        assert(_moduleIdWrapperFromWrappedId.vendorId == ModuleIdWrapper.VENDOR_ID_FOR_PRIMARY_MODULE_ID)
    }
}