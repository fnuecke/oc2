/* MIT licensed, see LICENSE.
 *
 * Drives a redstone interface through the mid-level API.
 *
 *   tcc -run redstone.c                 wait for an input change, print it
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

#define REDSTONE_CHANGED 1

int main(int argc, char **argv) {
    if (argc > 3) {
        printf("usage: %s [side [level]]\n", argv[0]);
        return 1;
    }

    if (mlapi_open() < 0) {
        printf("no mid-level API, or not running as root\n");
        return 1;
    }

    const int redstone = mlapi_find(0, "REDSTN");
    if (redstone < 0) {
        printf("no redstone interface\n");
        return 1;
    }

    if (argc == 1) {
        mlapi_events_enable();
        printf("waiting for a redstone change...\n");
        fflush(stdout);
        for (;;) {
            mlapi_event_t event;
            while (mlapi_event_read(&event) == 0) {
                if (event.device == redstone && event.code == REDSTONE_CHANGED) {
                    mlapi_events_disable();
                    /* The low byte is the side, the high byte the level. */
                    printf("side %d: in = %d\n", event.value & 0xFF, event.value >> 8);
                    return 0;
                }
            }
            if (mlapi_events_wait() < 0) {
                printf("no interrupt\n");
                return 1;
            }
        }
    }

    const unsigned char side = atoi(argv[1]);
    int result;
    if (argc == 3) {
        const unsigned char arguments[] = {side, atoi(argv[2])};
        result = mlapi_call(redstone, SET_REDSTONE_OUTPUT, arguments, 2, NULL, 0);
        if (result < 0) goto failed;
        printf("side %d: out = %d\n", side, arguments[1]);
        return 0;
    }

    unsigned char out, in;
    result = mlapi_call(redstone, GET_REDSTONE_OUTPUT, &side, 1, &out, 1);
    if (result < 0) goto failed;
    result = mlapi_call(redstone, GET_REDSTONE_INPUT, &side, 1, &in, 1);
    if (result < 0) goto failed;
    printf("side %d: out = %d, in = %d\n", side, out, in);
    return 0;

failed:
    printf("device error %d\n", -result);
    return 1;
}
