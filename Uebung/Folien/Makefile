### Makefile to create all exercise sheets
###
### Authors: Demian Vöhringer
### Date 12.12.2019

################################################################################

# Output directories for all genereted files
OUT_DIR = _out/

# Folder for all temporary files
TEMP_DIR = .tmp/

DIRS = $(OUT_DIR) $(TEMP_DIR)

# LaTeX Build command
LATEXMKCI = mkdir -p $(DIRS); \
	  latexmk -pdf -output-directory=$(TEMP_DIR)

LATEXMK = $(LATEXMKCI) -silent


################################################################################

.PHONY: all FORCE clean move_all build_all ci

all: move_all

move_all: FORCE | build_all
	cp $(TEMP_DIR)*.pdf $(OUT_DIR)

ci: FORCE | build_ci
	cp $(TEMP_DIR)*.pdf $(OUT_DIR)

build_all: FORCE
	$(LATEXMK)

build_ci: FORCE
	$(LATEXMKCI)

FORCE:

clean:
	rm -rf $(OUT_DIR) $(TEMP_DIR)
	latexmk -C
	rm -rf *.nav *.snm *.vrb
