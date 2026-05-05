import { FaSearch, FaTrophy, FaUser } from "react-icons/fa";

export const howItWorks = [
  {
    title: "Create your player profile",
    body: "Show your game, role, and competitive identity in one trusted profile.",
    Icon: FaUser,
  },
  {
    title: "Verify rank and team status",
    body: "Submit proof, confirm if you are with a team, and earn visibility.",
    Icon: FaTrophy,
  },
  {
    title: "Get discovered with context",
    body: "Organizations can scout both active team players and available free agents.",
    Icon: FaSearch,
  },
];

export const supportedGames = [
  "Valorant",
  "League of Legends",
  "EA FC",
  "Rocket League",
  "Overwatch 2",
];

export const demoStats = [
  { value: "120+", key: "verifiedPlayers", label: "Verified Players" },
  { value: "10+", key: "organizations", label: "Organizations" },
];
