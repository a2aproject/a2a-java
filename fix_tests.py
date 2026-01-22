#!/usr/bin/env python3
import re
import sys
import glob

test_files = glob.glob('**/src/test/java/**/*Test.java', recursive=True)

for path in test_files:
    try:
        with open(path, 'r') as f:
            content = f.read()

        # Pattern to match create() calls with 5 arguments ending with executor or internalExecutor
        pattern = r'DefaultRequestHandler\.create\(\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*((?:executor|internalExecutor))\s*\)'

        def replacer(match):
            args = [match.group(i).strip() for i in range(1, 6)]
            executor = args[4]
            return f'DefaultRequestHandler.create({args[0]}, {args[1]}, {args[2]}, {args[3]}, {executor}, {executor})'

        new_content = re.sub(pattern, replacer, content, flags=re.MULTILINE | re.DOTALL)

        if new_content != content:
            with open(path, 'w') as f:
                f.write(new_content)
            print(f'Fixed: {path}')
    except Exception as e:
        print(f'Error processing {path}: {e}')
