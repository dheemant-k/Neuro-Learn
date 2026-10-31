import supermanImg from "../assets/superman.png";
import supergirlImg from "../assets/supergirl.png";
import batmanImg from "../assets/batman.png";

export const characters = [
  { id: "superman", name: "Superman", image: supermanImg, personality: "hope & solar power" },
  { id: "supergirl", name: "Supergirl", image: supergirlImg, personality: "speed & cosmic energy" },
  { id: "batman", name: "Batman", image: batmanImg, personality: "tactical precision & deduction" },
];

export const chapters = [
  {
    id: "linear_eq_2var",
    title: "Linear Equations with Two Variables",
    description: "Master the art of solving equations with two unknowns through epic quests",
    topic: "linear equations in two variables",
    sections: [
      {
        id: "intro",
        title: "Introduction to Linear Equations",
        topic: "basic concepts of linear equations in two variables, what ax+by=c means, identifying linear equations",
        description: "Learn what linear equations look like and why they matter",
      },
      {
        id: "graphing",
        title: "Graphing on Coordinate Plane",
        topic: "plotting linear equations on the coordinate plane, finding x and y intercepts, slope of a line",
        description: "Plot equations and see them come alive on graphs",
      },
      {
        id: "pair_of_equations",
        title: "Pair of Linear Equations",
        topic: "understanding systems of two linear equations, consistent vs inconsistent systems, dependent systems",
        description: "What happens when two lines meet? Discover systems of equations",
      },
      {
        id: "substitution",
        title: "Solving by Substitution",
        topic: "substitution method for solving a pair of linear equations in two variables step by step",
        description: "Replace and conquer — solve systems by substituting one variable",
      },
      {
        id: "elimination",
        title: "Solving by Elimination",
        topic: "elimination method for solving a pair of linear equations in two variables step by step",
        description: "Add or subtract equations to eliminate a variable and find the answer",
      },
      {
        id: "word_problems",
        title: "Real-World Word Problems",
        topic: "word problems that translate to a pair of linear equations in two variables, real-life applications",
        description: "Apply your skills to solve real-world problems with two unknowns",
      },
    ],
    finalBoss: {
      id: "final_boss",
      title: "Final Boss — Apex Mainframe Core",
      description: "5 mastery challenges to conquer the linear systems mainframe!",
      questionCount: 5,
    },
  },
  {
    id: "quadratic_equations",
    title: "Quadratic Equations & Parabolas",
    description: "Master second-degree equations, gravitational parabolas, factoring, and the quadratic formula",
    topic: "quadratic equations and parabolas",
    sections: [
      {
        id: "quad_intro",
        title: "Introduction to Quadratics",
        topic: "standard form ax^2 + bx + c = 0, identifying quadratic equations, degree of polynomial",
        description: "Discover second-degree polynomials and understand the quadratic standard form",
      },
      {
        id: "quad_factoring",
        title: "Solving by Factoring",
        topic: "factoring quadratic expressions, splitting the middle term, zero product property",
        description: "Split the middle term and apply the zero-product rule to find roots",
      },
      {
        id: "quad_formula",
        title: "The Quadratic Formula",
        topic: "universal quadratic formula, calculating x = (-b ± √(b^2 - 4ac)) / (2a)",
        description: "Harness the universal formula to solve any quadratic equation effortlessly",
      },
      {
        id: "quad_discriminant",
        title: "The Discriminant & Roots",
        topic: "discriminant b^2 - 4ac, nature of roots, distinct real roots, equal roots, complex roots",
        description: "Calculate Δ to predict the exact number and nature of equation roots",
      },
      {
        id: "quad_graphing",
        title: "Graphing Parabolas",
        topic: "vertex form, axis of symmetry, finding vertex (h, k), trajectory apex",
        description: "Plot parabolic trajectories and pinpoint vertices and axes of symmetry",
      },
      {
        id: "quad_word_problems",
        title: "Kinematics & Projectiles",
        topic: "projectile motion, vertical height curves, trajectory apex, real-world quadratic models",
        description: "Calculate rocket flight paths, gravitational arcs, and physical dimensions",
      },
    ],
    finalBoss: {
      id: "quad_final_boss",
      title: "Final Boss — Gravitational Singularity",
      description: "Confront the cosmic gravitational matrix and solve 5 quadratic mastery challenges!",
      questionCount: 5,
    },
  },
];
