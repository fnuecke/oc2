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
long read(int fd, void *buffer, unsigned long count);
long write(int fd, const void *buffer, unsigned long count);
void *mmap(void *address, size_t length, int protection, int flags, int fd, long offset);
int usleep(unsigned int microseconds);

#define OCSEL 0x00 /* read the selected device, write to select one */
#define OCFUN 0x01 /* write a function code, which begins a transaction */
#define OCDAT 0x02 /* argument bytes in, result bytes or the error code out */
#define OCSTA 0x03 /* read the status bits, write OCEXEC or OCABRT */
#define OCEVC 0x04 /* read the event status bits, write OCEVQ to queue events */
#define OCEVD 0x05 /* next byte of the oldest event, 0xFF when none is queued */

#define BCNT 0x11 /* device count */
#define BSEL 0x12 /* selected entry */
#define BCLS 0x13 /* class of the selected entry */
#define BATR 0x14 /* device index to write to OCSEL */
#define BPRT 0x15 /* register offset in the page */
#define BNAM 0x17 /* names, one character per read, each 0 terminated, an empty one ends the list; any write rewinds */

#define CLSOC 0x80 /* the class every mid-level API device reports */

#define OCBUSY 0x01 /* the call has not finished yet */
#define OCDAV 0x02  /* a result byte is waiting at OCDAT */
#define OCERR 0x80  /* the call failed; OCDAT holds the code */

#define OCEVQ 0x01 /* queue events */
#define OCEVI 0x02 /* raise the interrupt while events are queued */
#define OCEVO 0x40 /* events were dropped, the queue was full */
#define OCEVP 0x80 /* an event is waiting at OCEVD */

#define OCEXEC 0x01 /* run the transaction */
#define OCABRT 0x00 /* abandon it; a call already running still has its effect */

#define OCENOD 0x01 /* no such device */
#define OCENOF 0x02 /* no such function */
#define OCEARG 0x05 /* not the arguments the function wanted */
#define OCEINT 0x06 /* the device failed internally */

#define MLAPI_MAX_DATA 256 /* at most this many argument and result bytes */

static volatile unsigned char *mlapi_io; /* the register page, after mlapi_open() */
static int mlapi_fd = -1;

/* Maps the register page into mlapi_io.
 * Returns 0, or -1 if there is none or it cannot be opened. */
static int mlapi_open(void) {
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
            return -1;
        }
        void *page = mmap(NULL, 4096, 3 /* PROT_READ | PROT_WRITE */, 1 /* MAP_SHARED */, fd, 0);
        if (page == (void *) -1) {
            close(fd);
            return -1;
        }
        mlapi_io = page;
        mlapi_fd = fd;
        return 0;
    }
    return -1;
}

/* Returns whether the selected device has the specified name. */
static int mlapi_has_name(const char *name) {
    mlapi_io[BNAM] = 0;
    int c;
    while ((c = mlapi_io[BNAM]) != 0) {
        const char *p = name;
        while (c != 0 && c == (unsigned char) *p) {
            p++;
            c = mlapi_io[BNAM];
        }
        if (c == 0 && *p == 0) {
            return 1;
        }
        while (c != 0) {
            c = mlapi_io[BNAM];
        }
    }
    return 0;
}

static int mlapi_find_all(int nth, const char *const *names) {
    const int count = mlapi_io[BCNT];
    for (int i = 0; i < count; i++) {
        mlapi_io[BSEL] = i;
        if (mlapi_io[BCLS] != CLSOC) {
            continue;
        }
        const char *const *name = names;
        while (*name && mlapi_has_name(*name)) {
            name++;
        }
        if (!*name && nth-- == 0) {
            return mlapi_io[BATR];
        }
    }
    return -1;
}

/* Finds the nth (0-based) device that has all the specified names: its device
 * name or the label set on a bus interface.
 * Returns its index for mlapi_call(), or -1 if there is no such device. */
#define mlapi_find(nth, ...) mlapi_find_all((nth), (const char *const[]) {__VA_ARGS__, NULL})

/* Calls a function of a device. Copies up to capacity result bytes into
 * results, which may be NULL. Returns the number of result bytes the device
 * sent, or the negated error code. */
static int mlapi_call(int device, int function,
                      const void *arguments, int argument_count,
                      void *results, int capacity) {
    mlapi_io[OCSEL] = device;
    mlapi_io[OCFUN] = function;
    for (int i = 0; i < argument_count; i++) {
        mlapi_io[OCDAT] = ((const unsigned char *) arguments)[i];
    }
    mlapi_io[OCSTA] = OCEXEC;
    while (mlapi_io[OCSTA] & OCBUSY) {
        usleep(1000);
    }
    if (mlapi_io[OCSTA] & OCERR) {
        return -mlapi_io[OCDAT];
    }

    int count = 0;
    while (mlapi_io[OCSTA] & OCDAV) {
        const unsigned char value = mlapi_io[OCDAT];
        if (results && count < capacity) {
            ((unsigned char *) results)[count] = value;
        }
        count++;
    }
    return count;
}

typedef struct {
    int device; /* index, as returned by mlapi_find() */
    int code;
    int value;
} mlapi_event_t;

/* Discards queued events, then queues new ones and raises the interrupt
 * while any are queued. */
static void mlapi_events_enable(void) {
    mlapi_io[OCEVC] = 0;
    mlapi_io[OCEVC] = OCEVQ | OCEVI;
}

/* Stops queueing events and discards queued ones. */
static void mlapi_events_disable(void) {
    mlapi_io[OCEVC] = 0;
}

/* Returns whether an event is queued. */
static int mlapi_event_pending(void) {
    return (mlapi_io[OCEVC] & OCEVP) != 0;
}

/* Removes the oldest event from the queue.
 * Returns 0, or -1 if none is queued. */
static int mlapi_event_read(mlapi_event_t *event) {
    if (!mlapi_event_pending()) {
        return -1;
    }
    event->device = mlapi_io[OCEVD];
    event->code = mlapi_io[OCEVD];
    event->value = mlapi_io[OCEVD];
    event->value |= mlapi_io[OCEVD] << 8;
    return 0;
}

/* Sleeps until the next event is queued. May also return without one.
 * Needs events enabled, and the queue read until none is pending.
 * Returns 0, or -1 on failure. */
static int mlapi_events_wait(void) {
    int value = 1;
    if (write(mlapi_fd, &value, sizeof value) != sizeof value) {
        return -1;
    }
    return read(mlapi_fd, &value, sizeof value) == sizeof value ? 0 : -1;
}

#endif /* MLAPI_H */
