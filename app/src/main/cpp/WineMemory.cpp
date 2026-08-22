#include <cstdlib>
#include <cstdint>
#include <cstring>

static uint64_t parse_env_u64(
        const char *name,
        uint64_t fallback)
{
    const char *value =
            getenv(name);

    if (!value || !*value) {
        return fallback;
    }

    char *end = nullptr;

    unsigned long long result =
            strtoull(
                    value,
                    &end,
                    10);

    if (end == value) {
        return fallback;
    }

    return static_cast<uint64_t>(
            result);
}

extern "C"
uint64_t winlator_memory_physical_ram()
{
    return parse_env_u64(
            "WINLATOR_PHYSICAL_RAM",
            0);
}

extern "C"
uint64_t winlator_memory_available_ram()
{
    return parse_env_u64(
            "WINLATOR_AVAILABLE_RAM",
            0);
}

extern "C"
uint64_t winlator_memory_swap_total()
{
    return parse_env_u64(
            "WINLATOR_SWAP_TOTAL",
            0);
}

extern "C"
uint64_t winlator_memory_swap_free()
{
    return parse_env_u64(
            "WINLATOR_SWAP_FREE",
            0);
}

extern "C"
uint64_t winlator_memory_reported_total()
{
    uint64_t physical =
            winlator_memory_physical_ram();

    uint64_t swap =
            winlator_memory_swap_total();

    const char *mode =
            getenv("WINLATOR_MEMORY_MODE");

    if (mode &&
        strcmp(mode, "ram+swap") == 0) {

        return physical + swap;
    }

    return physical;
}

extern "C"
uint64_t winlator_memory_reported_available()
{
    uint64_t available =
            winlator_memory_available_ram();

    uint64_t swapFree =
            winlator_memory_swap_free();

    const char *mode =
            getenv("WINLATOR_MEMORY_MODE");

    if (mode &&
        strcmp(mode, "ram+swap") == 0) {

        return available + swapFree;
    }

    return available;
}

extern "C"
int winlator_memory_is_enabled()
{
    const char *mode =
            getenv("WINLATOR_MEMORY_MODE");

    return mode &&
           strcmp(mode, "ram+swap") == 0;
}
