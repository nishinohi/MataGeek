package com.example.matageek.fruity.types

class FmTypes {
    companion object {
        const val MAX_DATA_SIZE_PER_WRITE: Int = 20
    }

    enum class ModuleId(val id: Byte) {
        //Standard modules
        NODE(0), // Not a module per se), but why not let it send module messages
        BEACONING_MODULE(1),
        SCANNING_MODULE(2),
        STATUS_REPORTER_MODULE(3),
        DFU_MODULE(4),
        ENROLLMENT_MODULE(5),
        IO_MODULE(6),
        DEBUG_MODULE(7),
        CONFIG(8),

        //BOARD_CONFIG(9), //deprecated as of 20.01.2020 (boardconfig is not a module anymore)
        MESH_ACCESS_MODULE(10),

        //MANAGEMENT_MODULE=11), //deprecated as of 22.05.2019
        TESTING_MODULE(12),
        BULK_MODULE(13),
        ENVIRONMENT_SENSING_MODULE(14), //Placeholder for environmental sensing module

        //M-Way Modules
        CLC_MODULE(150.toByte()),
        VS_MODULE(151.toByte()),
        ENOCEAN_MODULE(152.toByte()),
        ASSET_MODULE(153.toByte()),
        EINK_MODULE(154.toByte()),
        WM_MODULE(155.toByte()),
        ET_MODULE(156.toByte()), //Placeholder for Partner
        MODBUS_MODULE(157.toByte()),
        BP_MODULE(158.toByte()),

        //Other Modules), this range can be used for experimenting but must not be used if FruityMesh
        //nodes are to be used in a network with nodes of different vendors as their moduleIds will clash
        MY_CUSTOM_MODULE(200.toByte()),

        //PING_MODULE(201), Deprecated as of 26.08.2020, now uses a VendorModuleId
        //VENDOR_TEMPLATE_MODULE(202), Deprecated as of 20.08.2020, now uses a VendorModuleId
        SIG_EXAMPLE_MODULE(203.toByte()),
        MATAGEEK_MODULE(204.toByte()),

        //The VendorModuleId was introduced to have a range of moduleIds that do not clash
        //between different vendors
        VENDOR_MODULE_ID_PREFIX(240.toByte()), //0xF0

        //The in between space is reserved for later extensions, e.g. of the vendor module ids
        //Invalid Module: 0xFF is the flash memory default and is therefore invalid
        INVALID_MODULE(255.toByte()),
    }
}