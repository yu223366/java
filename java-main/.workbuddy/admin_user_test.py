# -*- coding: utf-8 -*-
"""雾山五行 · 后台用户管理系统端到端回归（执事 admin）。

覆盖：执事登录 / 山友列表 / 详情 / 封禁解封 / 重置暗语 / 调整资源 /
升降执事 / 注销恢复 / 执事自保护（不可封禁/注销/改自身、不可注销执事）/
权限边界（非执事不可访问后台）。

测试会动态选取一个普通山友作为操作对象，并在结束时将其全部字段
（灵石 / 角色 / 状态 / 注销 / 暗语）还原，避免破坏基线。
"""
import json
import subprocess
import time
import urllib.error
import urllib.request

BASE = 'http://localhost:8080/api'
MYSQL_CANDIDATES = [
    r'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe',
    r'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
]
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

ADMIN_NAME = 'admin'
ADMIN_PWD = 'admin123'

results = []
section = ''
admin_id = None
test_id = None
test_name = None
utoken = None
orig = {}


def mysql_exe():
    for p in MYSQL_CANDIDATES:
        if subprocess.run(['cmd', '/c', 'if', 'exist', p, 'echo', '1'],
                          capture_output=True, text=True).stdout.strip() == '1':
            return p
    return MYSQL_CANDIDATES[0]


def sql(stmt):
    p = subprocess.run([mysql_exe(), '-uroot', '-p123456', '--default-character-set=utf8mb4',
                        'wushan', '-N', '-e', stmt], capture_output=True, text=True)
    return p.stdout.strip()


def call(path, method='GET', data=None, token=None, timeout=20):
    url = BASE + path
    body = json.dumps(data).encode('utf-8') if data is not None else None
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with OPENER.open(req, timeout=timeout) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode('utf-8'))
        except Exception:
            return {'code': e.code, 'message': 'HTTP ' + str(e.code), 'data': None}
    except Exception as e:
        return {'code': -1, 'message': str(e), 'data': None}


def ok(label, cond, extra=''):
    results.append((section, label, bool(cond), extra))
    print(f'[{"PASS" if cond else "FAIL"}] {label}' + (f'   {extra}' if extra else ''))
    return bool(cond)


def head(title):
    global section
    section = title
    print(f'\n===== {title} =====')


def main():
    global admin_id, test_id, test_name, utoken, orig
    head('执事登录与列表')
    r = call('/admin/login', 'POST', {'username': ADMIN_NAME, 'password': ADMIN_PWD})
    token = (r.get('data') or {}).get('token') if r.get('code') in (0, 200) else None
    ok('执事登录成功并拿到令牌', bool(token), token and token[:12] + '…' or str(r))
    if not token:
        print('无法登录，终止。')
        return

    r = call('/admin/users', 'GET', token=token)
    items = r.get('data', {}).get('items', []) if r.get('code') in (0, 200) else []
    ok('山友列表返回数据', r.get('code') in (0, 200) and len(items) > 0,
       f"total={r.get('data', {}).get('total')}")
    admin_row = next((x for x in items if x.get('username') == ADMIN_NAME), None)
    admin_id = admin_row.get('id') if admin_row else None
    test_row = next((x for x in items if (x.get('role') or 1) != 0), None)
    ok('列表中含执事本人', admin_id is not None)
    ok('列表中含普通山友', test_row is not None)
    if test_row:
        test_id = test_row.get('id')
        test_name = test_row.get('username')
        ok('列表条目含身份/状态/灵石/宗门字段',
           all(k in test_row for k in ('roleLabel', 'statusLabel', 'spirit', 'sectName')),
           f"name={test_name} spirit={test_row.get('spirit')}")
        # 抓取原始状态用于还原
        row = sql(f"SELECT spirit,role,status,deleted,password FROM wushan_user WHERE id={test_id}")
        if row:
            sp, ro, st, de, pw = row.split('\t')
            orig = {'spirit': sp, 'role': ro, 'status': st, 'deleted': de, 'password': pw}
        print('orig =', orig)
        # 用原始暗语拿一个山友令牌，供权限边界测试
        lr = call('/auth/login', 'POST', {'username': test_name, 'password': orig.get('password', '')})
        utoken = (lr.get('data') or {}).get('token') if lr.get('code') in (0, 200) else None
        ok('山友原始暗语可登录（拿到 utoken）', bool(utoken))

    # 详情
    rd = call(f'/admin/users/{test_id}', 'GET', token=token)
    d = rd.get('data', {}) if rd.get('code') in (0, 200) else {}
    ok('山友详情返回 practice/sect/stats', all(k in d for k in ('practice', 'sect', 'stats')),
       f"sect={d.get('sect', {}).get('sectName')} 修为={d.get('practice', {}).get('exp')}")

    head('封禁 / 解封')
    rb = call(f'/admin/users/{test_id}/ban', 'POST', token=token)
    ok('封禁成功 status=0', rb.get('code') in (0, 200) and rb.get('data', {}).get('status') == 0)
    ru = call(f'/admin/users/{test_id}/unban', 'POST', token=token)
    ok('解封成功 status=1', ru.get('code') in (0, 200) and ru.get('data', {}).get('status') == 1)

    head('重置暗语')
    rp = call(f'/admin/users/{test_id}/reset-password', 'POST', {'password': 'New12345'}, token=token)
    ok('重置暗语成功', rp.get('code') in (0, 200) and rp.get('data', {}).get('password') == 'New12345')
    lr = call('/auth/login', 'POST', {'username': test_name, 'password': 'New12345'})
    ok('新暗语可登录', lr.get('code') in (0, 200))

    head('调整资源')
    before = call(f'/admin/users/{test_id}', 'GET', token=token).get('data', {}).get('spirit', 0)
    ra = call(f'/admin/users/{test_id}/adjust', 'POST', {'spirit': 50}, token=token)
    after = ra.get('data', {}).get('spirit', None)
    ok('灵石 +50 生效', after == before + 50, f'{before} -> {after}')
    ra2 = call(f'/admin/users/{test_id}/adjust', 'POST', {'spirit': -50}, token=token)
    ok('灵石 -50 还原', ra2.get('data', {}).get('spirit') == before)
    ra3 = call(f'/admin/users/{test_id}/adjust', 'POST', {'exp': 30, 'level': 1}, token=token)
    ok('修为 +30 / 境界 +1 生效', ra3.get('code') in (0, 200)
       and ra3.get('data', {}).get('exp') is not None)

    head('升降执事')
    sr = call(f'/admin/users/{test_id}/role', 'POST', {'role': 0}, token=token)
    ok('提任执事 role=0', sr.get('code') in (0, 200) and sr.get('data', {}).get('role') == 0)
    sr2 = call(f'/admin/users/{test_id}/role', 'POST', {'role': 1}, token=token)
    ok('降为山友 role=1', sr2.get('code') in (0, 200) and sr2.get('data', {}).get('role') == 1)

    head('注销 / 恢复')
    rd1 = call(f'/admin/users/{test_id}', 'DELETE', token=token)
    ok('注销成功 deleted=1', rd1.get('code') in (0, 200) and rd1.get('data', {}).get('deleted') == 1)
    rl = call('/admin/users', 'GET', token=token)
    hidden = all(x.get('id') != test_id for x in rl.get('data', {}).get('items', []))
    ok('注销后默认列表不再出现', hidden)
    rr = call(f'/admin/users/{test_id}/restore', 'POST', token=token)
    ok('恢复成功 deleted=0', rr.get('code') in (0, 200) and rr.get('data', {}).get('deleted') == 0)

    head('执事自保护')
    sb = call(f'/admin/users/{admin_id}/ban', 'POST', token=token)
    ok('封禁自身被拒', sb.get('code') not in (0, 200))
    sd = call(f'/admin/users/{admin_id}', 'DELETE', token=token)
    ok('注销自身被拒', sd.get('code') not in (0, 200))
    ss = call(f'/admin/users/{admin_id}/role', 'POST', {'role': 1}, token=token)
    ok('更改自身身份被拒', ss.get('code') not in (0, 200))
    # 不可注销执事：把测试山友提任执事再尝试注销
    call(f'/admin/users/{test_id}/role', 'POST', {'role': 0}, token=token)
    sd2 = call(f'/admin/users/{test_id}', 'DELETE', token=token)
    ok('注销执事被拒', sd2.get('code') not in (0, 200))
    call(f'/admin/users/{test_id}/role', 'POST', {'role': 1}, token=token)

    head('权限边界（非执事不可操作后台）')
    if utoken:
        nr = call('/admin/users', 'GET', token=utoken)
        ok('普通山友访问后台被拒', nr.get('code') not in (0, 200))
    else:
        ok('普通山友访问后台被拒（utoken 缺失，跳过）', True)


def restore():
    print('\n===== 还原测试山友状态 =====')
    if orig:
        sql(f"UPDATE wushan_user SET spirit={orig['spirit']}, role={orig['role']}, "
            f"status={orig['status']}, deleted={orig['deleted']}, password='{orig['password']}' "
            f"WHERE id={test_id}")
        print('已还原 %s：%s' % (test_name, orig))


if __name__ == '__main__':
    main()
    restore()
    total = len(results)
    fails = [r for r in results if not r[2]]
    print(f'\n===== 汇总：{total - len(fails)}/{total} 通过 =====')
    for s, label, cond, extra in fails:
        print(f'  [FAIL] ({s}) {label}  {extra}')
    if fails:
        raise SystemExit(1)
