; MIT licensed, see LICENSE.
;
; OCAPI -- call OpenComputers devices from a program of your own.
;
; INCLUDE DEVLIB.INC before this; the enumeration window constants come from
; there. Every OpenComputers device sits behind one shared port, so DEVFIND
; cannot pick one out: they all report the same class. Use OCFIND, which
; matches on the name instead.
;
; Not re-entrant. The port holds one selection and one transaction, so do not
; call it from an interrupt handler.

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
OCSEL	equ	0
OCFUN	equ	1
OCDAT	equ	2
OCSTA	equ	3

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

; OCABRT discards the result; it does not cancel the call. A synchronized call
; already handed to the host still runs and still has its effect on the world.
; The device stays busy until it is done, and the result is then thrown away.

; OCFIND -- locate the B'th device named at HL (B is 0-based; the name is 0
; terminated and at most six characters).
; exit  NC, A = port base, E = sub-device index
;       CY      no such device
; Write E to OCSEL to select the device, then drive the registers from
; the port base in A. A synchronised call costs a game tick, so poll OCSTA
; rather than assuming a result is there.
;
; E is the attribute byte, which is what OCSEL takes (OC devs all share the
; same port).
OCFIND:
	in	a,(BCNT)
	or	a
	scf
	ret	z       ; return with carry set if no devices at all
	ld	d,a
	ld	e,0
OCFIND1:
	ld	a,e
	out	(BSEL),a
	in	a,(BCLS)
	cp	CLSOC
	jr	nz,OCFIND4
	push	hl
	xor	a
	out	(BNAM),a	; rewind the name stream
OCFIND2:
	in	a,(BNAM)
	cp	(hl)
	jr	nz,OCFIND3
	or	a		; both ran out at the same place, so it matched
	jr	z,OCFIND5
	inc	hl
	jr	OCFIND2
OCFIND3:
	pop	hl
OCFIND4:
	inc	e
	ld	a,e
	cp	d
	jr	c,OCFIND1
	scf
	ret
OCFIND5:
	pop	hl
	ld	a,b		; index check
	or	a
	jr	z,OCFIND6
	dec	b
	jr	OCFIND4
OCFIND6:
	in	a,(BATT)	; the attribute byte is the sub-device index
	ld	e,a
	in	a,(BPRT)
	or	a
	ret
