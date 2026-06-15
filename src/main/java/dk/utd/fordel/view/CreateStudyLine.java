package dk.utd.fordel.view;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.utd.fordel.domain.StudyLine;
import dk.utd.fordel.domain.StudyLineAlreadyExists;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.view.utils.Field;

@Controller
public class CreateStudyLine {

    private static final Logger logger = Log.getLogger(CreateStudyLine.class);

    private final StudyLineManager studyLineManager;
    private final UnitOfWork unitOfWork;

    public CreateStudyLine(StudyLineManager studyLineManager, UnitOfWork unitOfWork) {
        this.studyLineManager = studyLineManager;
        this.unitOfWork = unitOfWork;
    }

    @GetMapping("/create-study-line")
    public String getCreateStudyLine(Model model) {
        model.addAttribute("title", "Create study line");
        model.addAttribute("lineName", new Field(
                "text", "lineName", "Study line name",
                null, null));
        return "create-study-line";
    }

    @PostMapping("/create-study-line")
    public String postCreateStudyLine(Model model,
                                      @RequestParam("lineName") String lineName) {
        model.addAttribute("title", "Create study line");

        boolean isValid = true;
        String lineNameMessage = null;

        StudyLine studyLine = null;
        try {
            studyLine = new StudyLine(lineName);
        } catch (StudyLine.InvalidStudyLine e) {
            lineNameMessage = e.getMessage();
            isValid = false;
        }

        if (isValid) {
            try (var uow = unitOfWork.begin()) {
                logger.info("UnitOfWork started: {}", uow);
                studyLineManager.create(studyLine);
                uow.commit();
            } catch (StudyLineAlreadyExists e) {
                lineNameMessage = "Study line already exists";
                isValid = false;
            } catch (UnitOfWork.UnitOfWorkException e) {
                throw Log.giveUp(logger, e, "Failed to create study line");
            }
        }

        if (!isValid) {
            model.addAttribute("lineName", new Field(
                    "text", "lineName", "Study line name",
                    lineName, lineNameMessage));
            return "create-study-line";
        }

        return "redirect:/";
    }
}