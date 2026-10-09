import { useState } from "react";
import PixelCursor from "./components/PixelCursor";
import HomePage from "./pages/HomePage";
import RoadmapPage from "./pages/RoadmapPage";
import LearnPage from "./pages/LearnPage";
import TestPage from "./pages/TestPage";
import FeedbackPage from "./pages/FeedbackPage";
import DashboardPage from "./pages/DashboardPage";
import AuthPage from "./pages/AuthPage";
import BossFightPage from "./pages/BossFightPage";
import { AuthProvider } from "./context/AuthContext";

// pages: home | roadmap | learn | test | feedback | dashboard | auth | boss
function AppContent() {
  const [page, setPage] = useState("home");
  const [character, setCharacter] = useState(null);
  const [chapter, setChapter] = useState(null);
  const [section, setSection] = useState(null);
  const [testResults, setTestResults] = useState(null);

  const goHome = () => {
    setPage("home");
    setChapter(null);
    setSection(null);
  };

  const goRoadmap = () => setPage("roadmap");

  const handleSelectChapter = (ch) => {
    setChapter(ch);
    setPage("roadmap");
  };

  const handleLearn = (sec) => {
    setSection(sec);
    setPage("learn");
  };

  const handleTest = (sec) => {
    setSection(sec);
    setPage("test");
  };

  const handleTestFinish = (results) => {
    setTestResults(results);
    setPage("feedback");
  };

  const handleFinalBoss = () => {
    setPage("boss");
  };

  return (
    <>
      <PixelCursor />
      {page === "home" && (
        <HomePage
          selectedCharacter={character}
          onSelectCharacter={setCharacter}
          onSelectChapter={handleSelectChapter}
          onDashboard={() => setPage("dashboard")}
          onGoAuth={() => setPage("auth")}
        />
      )}
      {page === "auth" && (
        <AuthPage onLoginSuccess={() => setPage("home")} />
      )}
      {page === "roadmap" && chapter && character && (
        <RoadmapPage
          chapter={chapter}
          character={character}
          onLearn={handleLearn}
          onTest={handleTest}
          onFinalBoss={handleFinalBoss}
          onBack={goHome}
        />
      )}
      {page === "learn" && section && character && (
        <LearnPage
          section={section}
          character={character}
          onGoTest={() => handleTest(section)}
          onBack={goRoadmap}
        />
      )}
      {page === "test" && section && character && (
        <TestPage
          section={section}
          character={character}
          onFinish={handleTestFinish}
          onBack={goRoadmap}
        />
      )}
      {page === "feedback" && section && character && testResults && (
        <FeedbackPage
          section={section}
          character={character}
          results={testResults}
          onContinue={goRoadmap}
        />
      )}
      {page === "boss" && character && (
        <BossFightPage
          character={character}
          onVictory={goRoadmap}
          onDefeat={goRoadmap}
          onBack={goRoadmap}
        />
      )}
      {page === "dashboard" && (
        <DashboardPage onBack={goHome} />
      )}
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
