/* MIT licensed, see LICENSE.
 *
 * Lists the devices of the mid-level API, one per line: the index to pass to
 * mlapi_call(), the device's name, as well as aliases (e.g. bus interface label).
 *
 *   tcc -run devices.c
 */

#include <mlapi.h>

int main(void) {
    if (mlapi_open() < 0) {
        printf("no mid-level API, or not running as root\n");
        return 1;
    }

    const int count = mlapi_io[BCNT];
    for (int i = 0; i < count; i++) {
        mlapi_io[BSEL] = i;
        if (mlapi_io[BCLS] != CLSOC) {
            continue;
        }

        printf("%d", mlapi_io[BATR]);
        mlapi_io[BNAM] = 0;
        int names = 0;
        int c;
        while ((c = mlapi_io[BNAM]) != 0) {
            printf("%s", names++ < 2 ? "\t" : ", ");
            do {
                putchar(c);
            } while ((c = mlapi_io[BNAM]) != 0);
        }
        putchar('\n');
    }
    return 0;
}
