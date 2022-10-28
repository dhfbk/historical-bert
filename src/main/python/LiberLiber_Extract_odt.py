import os
import re
from odf import text, teletype
from odf.opendocument import load


for root, dirs, files in os.walk("/pat/to/liberliber"):

	for name in files:
		if name.endswith(".odt"):
			linesToPrint = []
			try:
				textdoc = load(os.path.join(root, name))
			except:
				break
			allparas = textdoc.getElementsByType(text.P)
			
			for i in allparas:
				
				if teletype.extractText(i).startswith("TRATTO DA"):
					try:
						match = re.findall(r'\d{4}', teletype.extractText(i), re.DOTALL)
						match.sort()
						year = match[len(match)-1]
					except:
						year = 9999

				
				linesToPrint.append(teletype.extractText(i))
			

				if teletype.extractText(i).startswith("TITOLO:"):
					sourceSection = True
					lineSearchDate = teletype.extractText(i).strip()

					outputName = teletype.extractText(i).replace("TITOLO:", "")
					outputName = re.sub('[^a-zA-Z0-9 _]+', '', outputName)
					outputName = outputName.strip()
					outputName = outputName.replace(" ", "_")
					outputName = outputName.replace("__", "_")
					outputName = outputName[:100] + ".txt"


				if teletype.extractText(i).startswith("AUTORE:"):
					author = teletype.extractText(i).replace("AUTORE:", "")
					author = re.sub('[^a-zA-Z0-9 _]+', '', author)
					author = author.strip()
					author = author.replace(" ", "_")
					author = author.replace("__", "_")
					author = author[:100]

			print(year,author,outputName)

			with open ("LLout_odt/"+str(year)+"__"+str(author)+"__"+str(outputName), 'w') as outputFile:
				for l in linesToPrint:
					outputFile.write(l)
					outputFile.write("\n")

			outputFile.close()