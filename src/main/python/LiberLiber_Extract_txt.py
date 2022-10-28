import os
import re
from odf import text, teletype
from odf.opendocument import load
import zipfile
import io
import chardet  
import shutil

libeliberpath = "/path/to/liberliber/"

for root, dirs, files in os.walk(libeliberpath):
	for name in files:
		if "rtf" in root:
			continue
		if name.endswith(".zip"):
			compressedText = ""
			with zipfile.ZipFile(os.path.join(root, name)) as zf:
				listOfiles = zf.namelist()
				for elem in listOfiles:
					if elem.endswith("txt"):
						compressedText = elem
						
			if compressedText == "":
				continue

			if compressedText.startswith("."):
				continue
			if "/" in compressedText:
				continue

			tmpPath = "uncompress/tmpFile" #+compressedText
			with zipfile.ZipFile(os.path.join(root, name)) as z:
				with z.open(compressedText) as zf, open(tmpPath, 'wb') as f:
					shutil.copyfileobj(zf, f)


			rawdata = open(tmpPath, 'rb').read()
			result = chardet.detect(rawdata)
			charenc = result['encoding']

			try: 
				with open (tmpPath, 'r', encoding = charenc) as f:
					for line in f:
						
						break
			except:
				charenc = 'latin1'

				continue
			if charenc == "Windows-1254":
				continue



			sourceSection = False
			with open (tmpPath, 'r', encoding = charenc) as f:	
				lineSearchDate = ""
				linesToPrint = []
				for line in f:
					linesToPrint.append(line)
					if line.startswith("TRATTO DA"):
						sourceSection = True
						lineSearchDate = line.strip()
					
					if sourceSection == True:
						if line.startswith(" "):
							lineSearchDate = lineSearchDate + line.strip()
						if line.isspace():
							sourceSection = False
							try:
								match = re.findall(r'\d{4}', lineSearchDate, re.DOTALL)
								year = match[len(match)-1]
							except:
								year = 9999
							


					if line.startswith("TITOLO:"):
						titleFileName = line.replace("TITOLO:", "")
						titleFileName = re.sub('[^a-zA-Z0-9 _]+', '', titleFileName)
						titleFileName = titleFileName.strip()
						titleFileName = titleFileName.replace(" ", "_")
						titleFileName = titleFileName.replace("__", "_")
						titleFileName = titleFileName[:100] + ".txt"

					if line.startswith("AUTORE:"):
						author = line.replace("AUTORE:", "")
						author = re.sub('[^a-zA-Z0-9 _]+', '', author)
						author = author.strip()
						author = author.replace(" ", "_")
						author = author.replace("__", "_")
						author = author[:100]

				print(year,author,titleFileName)


				with open ("LLout_txt/"+str(year)+"__"+str(author)+"__"+str(titleFileName), "w") as outFile:
					for x in linesToPrint:
						outFile.write(x)
