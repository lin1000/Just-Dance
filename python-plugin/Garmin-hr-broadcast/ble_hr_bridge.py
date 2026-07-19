#!/usr/bin/env python3
"""
ble_hr_bridge.py — Garmin BLE Heart Rate → UDP bridge for Just Dance
Connects to a Garmin Forerunner 997 (or any BLE HRM) and streams
"bpm:NN" UDP packets to localhost:9999 for the Java game to consume.

Requirements:
    pip install bleak

Usage:
    python ble_hr_bridge.py                   # auto-scan for first HRM device
    python ble_hr_bridge.py <device_address>  # e.g. D4:AC:4B:00:11:22
"""

import asyncio
import socket
import struct
import sys

try:
    from bleak import BleakClient, BleakScanner
except ImportError:
    print("ERROR: bleak not installed.  Run:  pip install bleak")
    sys.exit(1)

HR_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb"
HR_CHAR_UUID    = "00002a37-0000-1000-8000-00805f9b34fb"
UDP_HOST        = "127.0.0.1"
UDP_PORT        = 9999
RECONNECT_DELAY = 5   # seconds between reconnect attempts

udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)


def on_hr_notification(_sender, data: bytearray):
    """Parse BLE Heart Rate Measurement (0x2A37) and forward over UDP."""
    flags = data[0]
    if flags & 0x01:          # bit 0 set → 16-bit HR value
        hr = struct.unpack_from("<H", data, 1)[0]
    else:                     # 8-bit HR value
        hr = data[1]
    msg = f"bpm:{hr}"
    udp_sock.sendto(msg.encode(), (UDP_HOST, UDP_PORT))
    print(f"\r❤  {hr:>3} bpm", end="", flush=True)


async def find_hrm_device() -> str | None:
    """Scan for all BLE devices, then pick one that looks like a Garmin HRM."""
    print("Scanning for all BLE devices (10 s)...")
    # Broad scan — no service UUID filter because Garmin doesn't always
    # advertise service UUIDs in its broadcast packets.
    all_devices = await BleakScanner.discover(timeout=10.0)

    if not all_devices:
        print("\nNo BLE devices found at all.")
        print("  - Enable Bluetooth on this PC.")
        return None

    print(f"Found {len(all_devices)} BLE device(s):")
    for d in all_devices:
        print(f"  {d.name or '(unknown)':<30} [{d.address}]")

    # Prefer devices whose name contains "Garmin" or "Forerunner"
    garmin = [d for d in all_devices
              if d.name and any(k in d.name for k in ("Garmin", "Forerunner", "FR"))]
    if garmin:
        chosen = garmin[0]
        print(f"\nAuto-selected Garmin device: {chosen.name} [{chosen.address}]")
        return chosen.address

    # Fall back: ask the user to pass the address manually
    print("\nNo Garmin device found by name.")
    print("  - Confirm broadcast HR is ON on the watch.")
    print("  - If you see your watch above, re-run with its address:")
    print("      python ble_hr_bridge.py <address>")
    return None


async def stream(address: str):
    """Connect, subscribe to HR notifications, and stream forever."""
    print(f"Connecting to {address}...")
    async with BleakClient(address, timeout=15.0) as client:
        print(f"Connected. Streaming HR → UDP {UDP_HOST}:{UDP_PORT}")
        await client.start_notify(HR_CHAR_UUID, on_hr_notification)
        try:
            while client.is_connected:
                await asyncio.sleep(1)
        except asyncio.CancelledError:
            pass
        finally:
            try:
                await client.stop_notify(HR_CHAR_UUID)
            except Exception:
                pass
    print("\nDevice disconnected.")


async def run(address: str | None):
    if address is None:
        address = await find_hrm_device()
        if address is None:
            return

    while True:
        try:
            await stream(address)
        except Exception as e:
            print(f"\nConnection error: {e}")
        print(f"Reconnecting in {RECONNECT_DELAY} s... (Ctrl+C to quit)")
        await asyncio.sleep(RECONNECT_DELAY)


if __name__ == "__main__":
    addr = sys.argv[1] if len(sys.argv) > 1 else None
    try:
        asyncio.run(run(addr))
    except KeyboardInterrupt:
        print("\nStopped.")
    finally:
        udp_sock.close()
