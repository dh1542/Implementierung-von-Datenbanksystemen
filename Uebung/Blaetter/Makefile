### Makefile to create all exercise sheets
###
### Authors: Sebastian Herbst, Peter Schwab
### Date: 2013-11-26
### 2019-06-19 Major rewrite by Demian Vöhringer

################################################################################
### define these macros here

# Structure of the tex files
# Top-level tex files are considered to be separed exercises
# If you want to split an exercise into multiple files, please consider using a
# directory.
#

# Suffixes:
START = UeIDB_
EXERCISE_SUFFIX = _Blatt
SOLUTION_SUFFIX = _ML
NOTES_SUFFIX = _Notizen
BEAMER_SUFFIX = _Beamer

# TexFiles:
INPUT_TEX = $(wildcard Inhalt/$(START)$(BLATT)*.tex)
EXERCISES_TEX = $(INPUT_TEX:Inhalt/%.tex=%$(EXERCISE_SUFFIX).tex)
SOLUTIONS_TEX = $(INPUT_TEX:Inhalt/%.tex=%$(SOLUTION_SUFFIX).tex)
NOTES_TEX     = $(INPUT_TEX:Inhalt/%.tex=%$(NOTES_SUFFIX).tex)
BEAMER_TEX    = $(INPUT_TEX:Inhalt/%.tex=%$(BEAMER_SUFFIX).tex)

TEXFILES      = $(filter %$(SUFFIX).tex,$(EXERCISES_TEX) $(SOLUTIONS_TEX) $(NOTES_TEX) $(BEAMER_TEX))
# Output directories for all genereted files
OUT_DIR = _out/

# Folder for all temporary files
TEMP_DIR = .tmp/

# Folder for simple exercise sheets and sheets with solutions
STUDON_DIR = $(OUT_DIR)StudOn/

# Folder for exercise sheets with solutions and notes
TUTOR_DIR = $(OUT_DIR)Tutor/

# PdfFiles:
EXERCISES_PDF = $(INPUT_TEX:Inhalt/%.tex=$(STUDON_DIR)%$(EXERCISE_SUFFIX).pdf)
SOLUTIONS_PDF = $(INPUT_TEX:Inhalt/%.tex=$(STUDON_DIR)%$(SOLUTION_SUFFIX).pdf)
NOTES_PDF     = $(INPUT_TEX:Inhalt/%.tex=$(TUTOR_DIR)%$(NOTES_SUFFIX).pdf)
BEAMER_PDF    = $(INPUT_TEX:Inhalt/%.tex=$(TUTOR_DIR)%$(BEAMER_SUFFIX).pdf)

PDFFILES     = $(filter %$(SUFFIX).pdf,$(EXERCISES_PDF) $(SOLUTIONS_PDF) $(NOTES_PDF) $(BEAMER_PDF))

# Copy Instructios
CP_EXERCISES = cp -f $(TEMP_DIR)$(START)$(BLATT)*$(EXERCISE_SUFFIX).pdf $(STUDON_DIR).
CP_SOLUTIONS = cp -f $(TEMP_DIR)$(START)$(BLATT)*$(SOLUTION_SUFFIX).pdf $(STUDON_DIR).
CP_NOTES = cp -f $(TEMP_DIR)$(START)$(BLATT)*$(NOTES_SUFFIX).pdf $(TUTOR_DIR).
CP_BEAMER = cp -f $(TEMP_DIR)$(START)$(BLATT)*$(BEAMER_SUFFIX).pdf $(TUTOR_DIR).

BLAETTER := 01 02 03 04 05 06 07 08 09 10 11 12 13 14

# LaTeX MK
LATEXMKCI = latexmk -pdf -output-directory=$(TEMP_DIR)

LATEXMK = $(LATEXMKCI) -silent

LATEXBUILDER = $(LATEXMK)
################################################################################
### Targets to build multiple files

.PHONY: all ci clean exercises notes solutions beamer $(BLAETTER) TEX PDFS bulid move_pdf
.PRECIOUS: $(TEMP_DIR)%.pdf

all: PDFS

ci: LATEXBUILDER=$(LATEXMKCI)
ci: move_pdf

# Recall makefile, because change of suffix changes variables
exercises: $(EXERCISES_PDF)

# Recall makefile, because change of suffix changes variables
notes: $(NOTES_PDF)

# Recall makefile, because change of suffix changes variables
solutions:  $(SOLUTIONS_PDF)

# Recall makefile, because change of suffix changes variables
beamer: $(BEAMER_PDF)

TEX: $(TEXFILES)

PDFS: $(PDFFILES)

build: $(TEMP_DIR) $(TEXFILES)
	$(LATEXBUILDER) $(START)$(BLATT)*$(SUFFIX).tex

move_pdf: $(STUDON_DIR) $(TUTOR_DIR) build
	$(CP_EXERCISES)
	$(CP_SOLUTIONS)
	$(CP_NOTES)
	$(CP_BEAMER)

################################################################################
### Targets to build pdf files

$(STUDON_DIR)%.pdf: $(TEMP_DIR)%.pdf $(STUDON_DIR)
	cp $< $@
$(TUTOR_DIR)%.pdf: $(TEMP_DIR)%.pdf $(TUTOR_DIR)
	cp $< $@

$(TEMP_DIR)%.pdf: %.tex $(TEMP_DIR)
	$(LATEXBUILDER) $<


$(TEMP_DIR) $(STUDON_DIR) $(TUTOR_DIR):
	mkdir -p $@

################################################################################
### Targets to build tex files

%$(EXERCISE_SUFFIX).tex: Inhalt/%.tex
	printf "\\input{Format/paper}\n\\input{Inhalt/$*.tex}\n" > $@
%$(SOLUTION_SUFFIX).tex: Inhalt/%.tex
	printf "\\input{Format/solution}\n\\input{Inhalt/$*.tex}\n" > $@
%$(BEAMER_SUFFIX).tex: Inhalt/%.tex
	printf "\\input{Format/beamer}\n\\input{Inhalt/$*.tex}\n" > $@
%$(NOTES_SUFFIX).tex: Inhalt/%.tex
	printf "\\input{Format/tutor}\n\\input{Inhalt/$*.tex}\n" > $@


################################################################################
### Targets to build one exercise

# Recall makefile, because change of BLATT changes variables
$(BLAETTER):
	$(MAKE) $(THIS_FILE) PDFS BLATT=$@

%_brochure.pdf: %
	pdf2ps $(TUTOR_DIR)$(START)$**$(NOTES_SUFFIX).pdf $(TEMP_DIR)$(START)$*$(NOTES_SUFFIX).ps
	psbook $(TEMP_DIR)$(START)$*$(NOTES_SUFFIX).ps $(TEMP_DIR)$(START)$*$(NOTES_SUFFIX)_sorted.ps
	psnup -2 $(TEMP_DIR)$(START)$*$(NOTES_SUFFIX)_sorted.ps $(TEMP_DIR)$(START)$*$(NOTES_SUFFIX)_brochure.ps
	ps2pdf $(TEMP_DIR)$(START)$*$(NOTES_SUFFIX)_brochure.ps $@

################################################################################
### Remaining Targets

clean:
	rm -rf $(OUT_DIR) $(TEMP_DIR) $(START)*
	-latexmk -C
	rm -f *.cpt
# END
