"""Lectura local del PDF del profesor; no ejecuta pruebas ni accede a la red."""
import hashlib
import json
import sys
from pathlib import Path
from pypdf import PdfReader

sys.stdout.reconfigure(encoding='utf-8', errors='replace')
source = next(Path('../..').glob('ESCENARIOS OBLIGATORIOS DE PRUEBAS*.pdf')).resolve()
destination = Path('perf/analisis/CIERRE-E8-E9-ACTUAL.txt').read_text(encoding='utf-8').strip()
pages = [page.extract_text() or '' for page in PdfReader(str(source)).pages]
text = '\n\n'.join(f'PAGINA {i + 1}\n{page}' for i, page in enumerate(pages))
Path(destination, 'requisito-profesor.txt').write_text(text, encoding='utf-8')
Path(destination, 'requisito-profesor-fuente.json').write_text(json.dumps({
    'archivo': str(source), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
    'paginas': len(pages), 'HTTP': 0, 'JMeter': 0
}, ensure_ascii=False, indent=2), encoding='utf-8')
print(text)
