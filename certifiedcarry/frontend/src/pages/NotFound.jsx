import { Link } from "react-router-dom";
import { Button } from "@heroui/react";
import { FaExclamationTriangle } from "react-icons/fa";
import useLanguage from "../hooks/useLanguage";

const NotFound = () => {
  const { t } = useLanguage();

  return (
    <section className="flex h-96 flex-col items-center justify-center bg-transparent px-4 text-center text-[#1d1d1d] dark:text-[#cae9ea]">
      <div className="bg-[#f4fbfb]/88 w-full max-w-3xl rounded-lg border border-[#3c4748]/55 p-8 shadow-xl backdrop-blur-sm dark:border-[#3c4748]/70 dark:bg-[#273b40]/65">
        <FaExclamationTriangle className="mb-4 text-6xl text-yellow-400" />
        <h1 className="cc-title-hero mb-4 text-5xl md:text-6xl">{t("notFound.title")}</h1>
        <p className="cc-body-lead mb-5">{t("notFound.body")}</p>
        <Button
          as={Link}
          to="/"
          className="cc-button-text mt-4 bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
        >
          {t("notFound.back")}
        </Button>
      </div>
    </section>
  );
};

export default NotFound;
