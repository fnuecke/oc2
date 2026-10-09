local harness = require("harness")

local source = io.open("/tmp/find_names.c", "w")
source:write([[
#include <mlapi.h>

int main(void) {
    if (mlapi_open() < 0) {
        return 1;
    }
    printf("%d %d %d %d %d\n",
           mlapi_find(0, "REDSTN"),
           mlapi_find(0, "test_device", "REDSTN"),
           mlapi_find(0, "oc2:redstone_interface", "test_device"),
           mlapi_find(0, "test_device", "nope"),
           mlapi_find(0, "test_device", "SYSTEM"));
    return 0;
}
]])
source:close()

local process = io.popen("tcc -I/mnt/builtin/include -run /tmp/find_names.c 2>&1")
local output = process:read("a")
process:close()
print(output)

local redstone, both, reversed, missing, split = output:match("^(%-?%d+) (%-?%d+) (%-?%d+) (%-?%d+) (%-?%d+)")

harness.expect("the redstone interface is found by name", redstone ~= nil and tonumber(redstone) >= 0, true)
harness.expect("several names find the same device", both, redstone)
harness.expect("in any order", reversed, redstone)
harness.expect("a name the device lacks finds nothing", missing, "-1")
harness.expect("names spread over two devices find nothing", split, "-1")

harness.report()
