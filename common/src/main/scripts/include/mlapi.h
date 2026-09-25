/* MIT licensed, see LICENSE.
 *
 * MLAPI - OpenComputers mid-level API library.
 *
 * Not re-entrant. Only one program must use this at a time.
 */

#ifndef MLAPI_H
#define MLAPI_H

#include <tcclib.h>

/* Not declared by tcclib.h. */
int open(const char *path, int flags, ...);
int close(int fd);
void *mmap(void *address, size_t length, int protection, int flags, int fd, long offset);
int usleep(unsigned int microseconds);

#define OCSEL 0x00 /* read the selected device, write to select one */
#define OCFUN 0x01 /* write a function code, which begins a transaction */
#define OCDAT 0x02 /* argument bytes in, result bytes or the error code out */
#define OCSTA 0x03 /* read the status bits, write OCEXEC or OCABRT */

#define BCNT 0x11 /* device count */
#define BSEL 0x12 /* selected entry */
#define BCLS 0x13 /* class of the selected entry */
#define BATR 0x14 /* device index to write to OCSEL */
#define BPRT 0x15 /* register offset in the page */
#define BNAM 0x17 /* name, one character per read, 0 ends; any write rewinds */

#define CLSOC 0x80 /* the class every mid-level API device reports */

#define OCBUSY 0x01 /* the call has not finished yet */
#define OCDAV 0x02  /* a result byte is waiting at OCDAT */
#define OCERR 0x80  /* the call failed; OCDAT holds the code */

#define OCEXEC 0x01 /* run the transaction */
#define OCABRT 0x00 /* abandon it; a call already running still has its effect */

#define OCENOD 0x01 /* no such device */
#define OCENOF 0x02 /* no such function */
#define OCEARG 0x05 /* not the arguments the function wanted */
#define OCEINT 0x06 /* the device failed internally */

#define MLAPI_MAX_DATA 256 /* at most this many argument and result bytes */

typedef volatile unsigned char *mlapi_t;

/* Maps the register page.
 * Returns NULL if there is none or it cannot be opened. */
static mlapi_t mlapi_open(void) {
    char path[64], name[16];
    for (int i = 0; i < 16; i++) {
        snprintf(path, sizeof path, "/sys/class/uio/uio%d/name", i);
        FILE *file = fopen(path, "r");
        if (!file) {
            continue;
        }
        const char *line = fgets(name, sizeof name, file);
        fclose(file);
        if (!line) {
            continue;
        }
        const char *expected = "oc2-mlapi\n";
        while (*expected && *line == *expected) {
            line++, expected++;
        }
        if (*expected || *line) {
            continue;
        }

        snprintf(path, sizeof path, "/dev/uio%d", i);
        const int fd = open(path, 2 /* O_RDWR */);
        if (fd < 0) {
            return NULL;
        }
        void *page = mmap(NULL, 4096, 3 /* PROT_READ | PROT_WRITE */, 1 /* MAP_SHARED */, fd, 0);
        close(fd);
        return page == (void *) -1 ? NULL : page;
    }
    return NULL;
}

/* Finds the nth (0-based) device with that name.
 * Returns its index for mlapi_call(), or -1 if there is no such device. */
static int mlapi_find(mlapi_t io, const char *name, int nth) {
    const int count = io[BCNT];
    for (int i = 0; i < count; i++) {
        io[BSEL] = i;
        if (io[BCLS] != CLSOC) {
            continue;
        }
        io[BNAM] = 0;
        const char *p = name;
        int c;
        while ((c = io[BNAM]) != 0 && c == *p) {
            p++;
        }
        if (c == 0 && *p == 0 && nth-- == 0) {
            return io[BATR];
        }
    }
    return -1;
}

/* Calls a function of a device. Copies up to capacity result bytes into
 * results, which may be NULL. Returns the number of result bytes the device
 * sent, or the negated error code. */
static int mlapi_call(mlapi_t io, int device, int function,
                      const void *arguments, int argument_count,
                      void *results, int capacity) {
    io[OCSEL] = device;
    io[OCFUN] = function;
    for (int i = 0; i < argument_count; i++) {
        io[OCDAT] = ((const unsigned char *) arguments)[i];
    }
    io[OCSTA] = OCEXEC;
    while (io[OCSTA] & OCBUSY) {
        usleep(1000);
    }
    if (io[OCSTA] & OCERR) {
        return -io[OCDAT];
    }

    int count = 0;
    while (io[OCSTA] & OCDAV) {
        const unsigned char value = io[OCDAT];
        if (results && count < capacity) {
            ((unsigned char *) results)[count] = value;
        }
        count++;
    }
    return count;
}

#endif /* MLAPI_H */
