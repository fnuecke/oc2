from harness import expect, report

try:
    import asyncio
    frozen = True
except ImportError:
    frozen = False

expect("frozen modules still resolve", frozen, True)

report()
