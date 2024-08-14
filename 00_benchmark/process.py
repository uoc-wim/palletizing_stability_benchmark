import subprocess
import shutil
import os

#mirrors the /jobs folder structure to jobs_done and results
def create_dirtree_without_files(src, dst):
    src = os.path.abspath(src)
    src_prefix = len(src) + len(os.path.sep)
    if not os.path.exists(dst):
        os.mkdir(dst)
    for(root, dirs, files) in os.walk(src):
        for dirname in dirs:
            dirpath = os.path.join(dst, root[src_prefix:], dirname)
            if not os.path.exists(dirpath):
                os.mkdir(dirpath)

create_dirtree_without_files('./jobs', './jobs_done')
create_dirtree_without_files('./jobs', './results')

for root, dirs, files in os.walk('./jobs', topdown=False):
    for name in files:
        subprocess.call([r'script.bat'])