package com.example.matageek.fruity.types

import junit.framework.TestCase

import org.junit.Test

class PrimitiveTypesTest : TestCase() {
    @Test
    fun test_ModuleIdWrapper() {
        val vendorModuleIdWrapperFromVendorId =
            ModuleIdWrapper(ModuleId.VENDOR_MODULE_ID_PREFIX.id, 1, 0xAB24.toShort())
        // VENDOR_MODULE_ID_PREFIX = 0xF0
        assert(vendorModuleIdWrapperFromVendorId.wrappedModuleId == 0xAB2401F0.toInt())
        assert(PrimitiveTypes.isVendorModuleId(vendorModuleIdWrapperFromVendorId.wrappedModuleId))
        val moduleIdWrapperFromPrimaryModuleId =
            ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id)
        assert(moduleIdWrapperFromPrimaryModuleId.prefix == ModuleId.STATUS_REPORTER_MODULE.id)
        assert(moduleIdWrapperFromPrimaryModuleId.subId == ModuleIdWrapper.SUB_ID_FOR_PRIMARY_MODULE_ID)
        assert(moduleIdWrapperFromPrimaryModuleId.vendorId == ModuleIdWrapper.VENDOR_ID_FOR_PRIMARY_MODULE_ID)
        assert(moduleIdWrapperFromPrimaryModuleId.wrappedModuleId == 0xFFFFFF03.toInt())
        assert(!PrimitiveTypes.isVendorModuleId(moduleIdWrapperFromPrimaryModuleId.wrappedModuleId))
        assert(moduleIdWrapperFromPrimaryModuleId.primaryModuleId == ModuleId.STATUS_REPORTER_MODULE.id)
        val vendorModuleIdWrapperFromWrappedId =
            ModuleIdWrapper(0xAB2401F0.toInt())
        assert(vendorModuleIdWrapperFromWrappedId.prefix == ModuleId.VENDOR_MODULE_ID_PREFIX.id)
        assert(vendorModuleIdWrapperFromWrappedId.subId == 1.toByte())
        assert(vendorModuleIdWrapperFromWrappedId.vendorId == 0xAB24.toShort())
        assert(PrimitiveTypes.isVendorModuleId(vendorModuleIdWrapperFromWrappedId.wrappedModuleId))
        val moduleIdWrapperFromWrappedId =
            ModuleIdWrapper(0xFFFFFF03.toInt())
        assert(moduleIdWrapperFromWrappedId.prefix == ModuleId.STATUS_REPORTER_MODULE.id)
        assert(moduleIdWrapperFromWrappedId.subId == ModuleIdWrapper.SUB_ID_FOR_PRIMARY_MODULE_ID)
        assert(moduleIdWrapperFromWrappedId.vendorId == ModuleIdWrapper.VENDOR_ID_FOR_PRIMARY_MODULE_ID)
        assert(!PrimitiveTypes.isVendorModuleId(moduleIdWrapperFromWrappedId.wrappedModuleId))
        assert(moduleIdWrapperFromWrappedId.primaryModuleId == ModuleId.STATUS_REPORTER_MODULE.id)
    }
}