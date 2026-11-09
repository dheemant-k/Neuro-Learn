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
import { AuthProvider, useAuth } from "./context/AuthContext";

// pages: home | roadmap | learn | test | feedback | dashboard | auth | boss
// Everything except home and auth needs a login (the backend enforces this too).
const PUBLIC_PAGES = new Set(["home", "auth"]);

function AppContent() {
  const { isAuthenticated, role, loading } = useAuth();
  const [page, setPage] = useState("home");
  const [character, setCharacter] = useState(null);
  const [chapter, setChapter] = useState(null);
  const [section, setSection] = useState(null);
  const [testResults, setTestResults] = useState(null);

  const [bossConfig, setBossConfig] = useState(null);

  const goHome = () => {
    setPage("home");
    setChapter(null);
    setSection(null);
    setBossConfig(null);
  };

  // Token expired or user logged out while on a protected page → show login instead.
  const view = !loading && !isAuthenticated && !PUBLIC_PAGES.has(page) ? "auth" : page;

  const goRoadmap = () => setPage("roadmap");

  const handleSelectChapter = (ch) => {
    if (!isAuthenticated) {
      setPage("auth");
      return;
    }
    if (role !== "STUDENT") {
      window.alert("Quests are for student accounts. Teachers and parents can follow progress from the dashboard.");
      return;
    }
    setChapter(ch);
    setPage("roadmap");
  };

  const handleOpenDashboard = () => setPage(isAuthenticated ? "dashboard" : "auth");

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

  const handleFinalBoss = (customBoss) => {
    setBossConfig(customBoss || chapter?.finalBoss);
    setPage("boss");
  };

  return (
    <>
      <PixelCursor />
      {view === "home" && (
        <HomePage
          selectedCharacter={character}
          onSelectCharacter={setCharacter}
          onSelectChapter={handleSelectChapter}
          onDashboard={handleOpenDashboard}
          onGoAuth={() => setPage("auth")}
        />
      )}
      {view === "auth" && (
        <AuthPage onLoginSuccess={() => setPage("home")} onBack={goHome} />
      )}
      {view === "roadmap" && chapter && character && (
        <RoadmapPage
          chapter={chapter}
          character={character}
          onLearn={handleLearn}
          onTest={handleTest}
          onFinalBoss={handleFinalBoss}
          onBack={goHome}
        />
      )}
      {view === "learn" && section && character && (
        <LearnPage
          key={section.id}
          section={section}
          character={character}
          onGoTest={() => handleTest(section)}
          onBack={goRoadmap}
        />
      )}
      {view === "test" && section && character && (
        <TestPage
          section={section}
          character={character}
          onFinish={handleTestFinish}
          onBack={goRoadmap}
        />
      )}
      {view === "feedback" && section && character && testResults && (
        <FeedbackPage
          section={section}
          character={character}
          results={testResults}
          onContinue={goRoadmap}
        />
      )}
      {view === "boss" && character && (
        <BossFightPage
          character={character}
          chapter={chapter}
          bossConfig={bossConfig}
          onVictory={goRoadmap}
          onDefeat={goRoadmap}
          onBack={goRoadmap}
        />
      )}
      {view === "dashboard" && (
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
