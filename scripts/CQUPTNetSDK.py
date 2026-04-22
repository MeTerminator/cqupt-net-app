import json
import random
import re
from dataclasses import dataclass

import requests


@dataclass
class AuthResult:
    result: str
    msg: str
    ret_code: int


@dataclass
class UnbindResult:
    result: str
    msg: str


@dataclass
class CheckerResult:
    result: str
    time: str
    msg: str


class CQUPTNetSDK:
    HOST = "192.168.200.2:801"
    REFERER = "http://192.168.200.2/"
    REQUEST_URL = "http://192.168.200.2:801/eportal/"

    C = "Portal"
    A = "login"
    A_UNBIND = "unbind_mac"
    CALLBACK = "dr1003"
    CALLBACK_UNBIND = "dr1002"
    LOGIN_METHOD = "1"
    JS_VERSION = "3.3.3"

    UA_MAP = {
        "mobile": "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Mobile/15E148 Safari/604.1",
        "desktop": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Safari/605.1.15",
    }

    ISP_MAP = ["telecom", "cmcc", "unicom", "xyw"]

    def __init__(
        self,
        stu_id: str,
        password: str,
        isp: str = "xyw",
        ua: str = "desktop",
        ip_addr: str = "",
        mac_addr: str = "000000000000",
    ):
        if isp not in self.ISP_MAP:
            raise ValueError(f"Invalid ISP: {isp}. Supported: {self.ISP_MAP}")
        if ua not in self.UA_MAP:
            raise ValueError(f"Invalid UA: {ua}. Supported: {list(self.UA_MAP.keys())}")

        self.stu_id = stu_id
        self.password = password
        self.isp = isp
        self.ua = ua
        self.ip_addr = ip_addr
        self.mac_addr = mac_addr
        self.is_logged_in = False

        self.session = requests.Session()
        self.session.verify = False

    def _get_ua(self, ua_key: str) -> str:
        return self.UA_MAP.get(ua_key, self.UA_MAP["desktop"])

    def _parse_jsonp(self, text: str) -> dict:
        start = text.find("(")
        end = text.rfind(")")
        if start != -1 and end != -1:
            return json.loads(text[start + 1 : end])
        return {}

    def check_status(self) -> bool:
        headers = {
            "User-Agent": self._get_ua(self.ua),
        }
        try:
            response = self.session.get(self.REFERER, headers=headers, timeout=5)
            text = response.text

            # 1. Check Login Status
            self.is_logged_in = "<title>注销页</title>" in text

            # 2. Extract IPv4
            patterns = [r"v4ip\s*=\s*['\"]([^'\"]+)['\"]", r"v46ip\s*=\s*['\"]([^'\"]+)['\"]"]
            for pattern in patterns:
                match = re.search(pattern, text)
                if match:
                    ip = match.group(1).strip().rstrip(".")
                    if ip and ip != "0.0.0.0" and ip != "000.000.000.000":
                        self.ip_addr = ip
                        break

            return self.is_logged_in
        except requests.exceptions.RequestException:
            return False

    def login(self, force: bool = False) -> AuthResult:
        # check_status updates self.is_logged_in and self.ip_addr
        self.check_status()

        if not force and self.is_logged_in:
            return AuthResult(result="1", msg="当前设备已登录", ret_code=0)

        if not self.ip_addr:
            return AuthResult(result="0", msg="未能获取到IPv4地址", ret_code=-1)

        device = 1 if self.ua != "desktop" else 0
        mac = self.mac_addr.replace(":", "")

        params = {
            "c": self.C,
            "a": self.A,
            "callback": self.CALLBACK,
            "login_method": self.LOGIN_METHOD,
            "user_account": f",{device},{self.stu_id}@{self.isp}",
            "user_password": self.password,
            "wlan_user_ip": self.ip_addr,
            "wlan_user_mac": mac,
            "jsVersion": self.JS_VERSION,
        }

        headers = {
            "Host": self.HOST,
            "Referer": self.REFERER,
            "User-Agent": self._get_ua(self.ua),
        }

        response = self.session.get(self.REQUEST_URL, params=params, headers=headers)
        result_dict = self._parse_jsonp(response.text)

        res = AuthResult(
            result=result_dict.get("result", ""),
            msg=result_dict.get("msg", ""),
            ret_code=int(result_dict.get("ret_code", 0)),
        )

        if res.result == "0" and res.ret_code == 2:
            res.msg = "当前设备已认证"

        return res

    def logout(self, force: bool = False) -> UnbindResult:
        # check_status updates self.is_logged_in and self.ip_addr
        self.check_status()

        if not force and not self.is_logged_in:
            return UnbindResult(result="1", msg="当前设备未登录")

        if not self.ip_addr:
            return UnbindResult(result="0", msg="未能获取到IPv4地址")

        mac = self.mac_addr.replace(":", "")
        params = {
            "c": self.C,
            "a": self.A_UNBIND,
            "callback": self.CALLBACK_UNBIND,
            "user_account": f"{self.stu_id}@{self.isp}",
            "wlan_user_mac": mac,
            "wlan_user_ip": self.ip_addr,
            "jsVersion": self.JS_VERSION,
            "v": random.randint(1000, 9999),
        }

        headers = {
            "Host": self.HOST,
            "Referer": self.REFERER,
            "User-Agent": self._get_ua(self.ua),
        }

        response = self.session.get(self.REQUEST_URL, params=params, headers=headers)
        result_dict = self._parse_jsonp(response.text)

        return UnbindResult(
            result=result_dict.get("result", ""), msg=result_dict.get("msg", "")
        )
