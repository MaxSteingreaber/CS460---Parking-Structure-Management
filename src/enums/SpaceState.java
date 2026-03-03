package enums;

public enum SpaceState {
    AVAILABLE,
    IN_TRANSIT,   // allocated and reserved internally; renders GREEN until weight sensor fires
    OCCUPIED,
    RESTRICTED,
    RESERVED
}