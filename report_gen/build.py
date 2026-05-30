# -*- coding: utf-8 -*-
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

from engine import Report
from front import build_front
from ch1 import build_ch1
from ch2 import build_ch2
from ch3 import build_ch3
from ch4 import build_ch4
from ch5 import build_ch5
from ch6 import build_ch6, build_appendix, build_references

R = Report()
build_front(R)
build_ch1(R)
build_ch2(R)
build_ch3(R)
build_ch4(R)
build_ch5(R)
build_ch6(R)
build_appendix(R)
build_references(R)

out = r"D:\AAA\Engvibes\BaoCao_QuanLy_CuaHang_VLXD.docx"
R.save(out)

# stats
doc = R.doc
paras = len(doc.paragraphs)
tables = len(doc.tables)
print(f"Saved: {out}")
print(f"Paragraphs: {paras}, Tables: {tables}")
