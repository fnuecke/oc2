/* MIT licensed, see LICENSE.
 *
 * Drives a redstone interface through the mid-level API.
 *
 *   tcc -run redstone.c <side>          print that side's levels
 *   tcc -run redstone.c <side> <level>  set that side's output level
 *
 * Sides are numbered as the redstone interface documentation describes, 0 is
 * down and 1 is up. Levels are 0 to 15.
 */

#include <mlapi.h>

#define GET_REDSTONE_INPUT 1
#define GET_REDSTONE_OUTPUT 2
#define SET_REDSTONE_OUTPUT 3

int main(int argc, char **argv) {
    if (argc < 2 || argc > 3) {
        printf("usage: %s <side> [level]\n", argv[0]);
        return 1;
    }

    mlapi_t io = mlapi_open();
    if (!io) {
        printf("no mid-level API, or not running as root\n");
        return 1;
    }

    const int redstone = mlapi_find(io, "REDSTN", 0);
    if (redstone < 0) {
        printf("no redstone interface\n");
        return 1;
    }

    const unsigned char side = atoi(argv[1]);
    int result;
    if (argc == 3) {
        const unsigned char arguments[] = {side, atoi(argv[2])};
        result = mlapi_call(io, redstone, SET_REDSTONE_OUTPUT, arguments, 2, NULL, 0);
        if (result < 0) goto failed;
        printf("side %d: out = %d\n", side, arguments[1]);
        return 0;
    }

    unsigned char out, in;
    result = mlapi_call(io, redstone, GET_REDSTONE_OUTPUT, &side, 1, &out, 1);
    if (result < 0) goto failed;
    result = mlapi_call(io, redstone, GET_REDSTONE_INPUT, &side, 1, &in, 1);
    if (result < 0) goto failed;
    printf("side %d: out = %d, in = %d\n", side, out, in);
    return 0;

failed:
    printf("device error %d\n", -result);
    return 1;
}
