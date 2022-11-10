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
START = UeIDB
EXERCISE_SUFFIX = _Blatt
SOLUTION_SUFFIX = _ML
NOTES_SUFFIX = _Notizen
BEAMER_SUFFIX = _Beamer
# Output directories for all genereted files
OUT_DIR = _out/

# Folder for all temporary files
TEMP_DIR = .tmp/

# Folder for simple exercise sheets and sheets with solutions
STUDON_DIR = $(OUT_DIR)StudOn/

# Folder for exercise sheets with solutions and notes
TUTOR_DIR = $(OUT_DIR)Tutor/

CP_EXERCISES = cp -f $(TEMP_DIR)$(START)$(FLAG)*$(EXERCISE_SUFFIX).pdf $(STUDON_DIR).
CP_SOLUTIONS = cp -f $(TEMP_DIR)$(START)$(FLAG)*$(SOLUTION_SUFFIX).pdf $(STUDON_DIR).
CP_NOTES = cp -f $(TEMP_DIR)$(START)$(FLAG)*$(NOTES_SUFFIX).pdf $(TUTOR_DIR).
CP_BEAMER = cp -f $(TEMP_DIR)$(START)$(FLAG)*$(BEAMER_SUFFIX).pdf $(TUTOR_DIR).

LATEXMKCI = latexmk -pdf -output-directory=$(TEMP_DIR)

LATEXMK = $(LATEXMKCI) -silent



################################################################################
### Targets to build multiple files

.PHONY: all ci clean

all: build_beamer build_exercises build_notes build_solutions
	$(MAKE) $(THIS_FILE) move_all

ci: | $(TEMP_DIR)
	$(LATEXMKCI)
	$(MAKE) $(THIS_FILE) move_all

exercises: build_exercises | $(STUDON_DIR)
	$(CP_EXERCISES)

notes: build_notes | $(TUTOR_DIR)
	$(CP_NOTES)

solutions: build_solutions | $(STUDON_DIR)
	$(CP_SOLUTIONS)

beamer: build_beamer | $(TUTOR_DIR)
	$(CP_BEAMER)

move_all: FORCE | $(STUDON_DIR) $(TUTOR_DIR)
	$(CP_EXERCISES)
	$(CP_SOLUTIONS)
	$(CP_NOTES)
	$(CP_BEAMER)

build_exercises: FORCE | $(TEMP_DIR)
	$(LATEXMK) *$(EXERCISE_SUFFIX).tex

build_notes: FORCE | $(TEMP_DIR)
	$(LATEXMK) *$(NOTES_SUFFIX).tex

build_solutions: FORCE | $(TEMP_DIR)
	$(LATEXMK) *$(SOLUTION_SUFFIX).tex

build_beamer: FORCE | $(TEMP_DIR)
	$(LATEXMK) *$(BEAMER_SUFFIX).tex

################################################################################
### Targets to build long or short

tex_long: FORCE
	./generate.sh

tex_short: FORCE
	./generate_short.sh

################################################################################
### Targets to build one exercise

%build: FORCE | $(TEMP_DIR)
	$(LATEXMK) $(START)$**.tex

01 02 03 04 05 06 07 08 09 10 11 12 13 14:
	$(MAKE) $(THIS_FILE) $@build
	$(MAKE) $(THIS_FILE) move_all

################################################################################
### Targets to build one file

$(STUDON_DIR)%.pdf: $(TEMP_DIR)%.pdf |$(STUDON_DIR)
	cp -f $(TEMP_DIR)$(@F) $@

$(TUTOR_DIR)%.pdf: $(TEMP_DIR)%.pdf | $(TUTOR_DIR)
	cp -f $(TEMP_DIR)$(@F) $@

$(TEMP_DIR)%.pdf: %.tex FORCE |$(TEMP_DIR)
	$(LATEXMK) $<

$(TEMP_DIR) $(STUDON_DIR) $(TUTOR_DIR):
	mkdir -p $@
################################################################################
### Remaining Targets

FORCE:

clean:
	rm -rf $(OUT_DIR) $(TEMP_DIR)
	latexmk -C
	rm -f *.cpt
# END

