; MIT licensed, see LICENSE.
;
; OCAPI -- call OpenComputers devices from a program of your own.
;
; INCLUDE DEVLIB.INC before this; the enumeration window constants and DEVFIND
; come from there. Every OpenComputers device sits behind one shared port and
; reports the same class, so select a specific one by name. OCFIND does that,
; and also returns the sub-device index OCSEL takes.
;
; Not re-entrant. The port holds one selection and one transaction, so do not
; call it from an interrupt handler. For those, there are the 'event' registers.

CLSOC	equ	80h		; the class every OpenComputers device reports

; Register offsets from the port base OCFIND returns.
;
;   +0  read  the selected sub-device
;       write select one; this resets the transaction
;   +1  write a function code, which begins a transaction
;             FFh is reserved and always faults
;   +2  read  the next result byte, or the error code once OCERR is set
;       write the next argument byte
;   +3  read  status, see the bits below
;       write OCEXEC to run the transaction, OCABRT to abandon it
;   +4  read  event status, see the OCEV bits below
;       write OCEVQ and OCEVI to switch events on; any write clears OCEVO
;   +5  read  the next byte of the oldest event, FFh when none is queued
OCSEL	equ	0
OCFUN	equ	1
OCDAT	equ	2
OCSTA	equ	3
OCEVC	equ	4
OCEVD	equ	5

OCBUSY	equ	00000001b	; the call has not finished yet
OCDAV	equ	00000010b	; a result byte is waiting at OCDAT
OCERR	equ	10000000b	; the call failed; OCDAT holds the code

; OCBUSY and OCERR are never set together. While OCBUSY is set every write
; except OCSTA is ignored, so wait for it to clear before starting anything;
; the busy bit is the whole signal.

OCEXEC	equ	01h		; run the transaction
OCABRT	equ	00h		; abandon it, see below

OCENOD	equ	01h		; no such sub-device
OCENOF	equ	02h		; no such function
OCEARG	equ	05h		; not the arguments the function wanted
OCEINT	equ	06h		; the device failed internally

OCEVQ	equ	00000001b	; queue events
OCEVI	equ	00000010b	; raise the interrupt while events are queued
OCEVO	equ	01000000b	; overflow indicator, events were dropped
OCEVP	equ	10000000b	; an event is pending at OCEVD

; An event is four bytes: the sub-device index, the event code, and a 16-bit
; value, low byte first. Reading the last byte consumes it. Switching OCEVQ off
; discards queued events. All devices share one interrupt; BINT reports its
; vector. The line stays raised while events are queued, a handler must read
; until OCEVP clears.

; OCABRT discards the result; it does not cancel the call. A synchronized call
; already handed to the host still runs and still has its effect on the world.
; The device stays busy until it is done, and the result is then thrown away.

; OCFIND -- locate the B'th device known by all names in the list at HL: its
; device name or the label set on a bus interface (B is 0-based; each name is
; 0 terminated, a repeated 0 indicates the end of the list).
; exit  NC, A = port base, E = sub-device index
;       CY      no such device
; Write E to OCSEL to select the device, then drive the registers from
; the port base in A. A synchronised call costs a game tick, so poll OCSTA
; rather than assuming a result is there.
;
; E is the attribute byte, which is what OCSEL takes (OC devs all share the
; same port).
OCFIND:
	ld	c,CLSOC
	call	DEVFIND
	ret	c
	push	af
	in	a,(BATT)	; the attribute byte is the sub-device index
	ld	e,a
	pop	af
	ret
